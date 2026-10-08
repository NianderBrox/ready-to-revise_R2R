import {
    BadRequestException,
    Injectable,
    UnauthorizedException,
    HttpException,
    HttpStatus,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import { UsersService } from '../../../users/application/services/users.service';
import { PrismaService } from '../../../../prisma/prisma.service';
import { MailService } from '../../../mail/application/services/mail.service';
import { JwtPayload } from '../../domain/interfaces/jwt-payload.interface';
import { AuthResponseDto } from '../../presentation/dto/auth-response.dto';
import { LoginDto } from '../../presentation/dto/login.dto';
import { RegisterDto } from '../../presentation/dto/register.dto';

@Injectable()
export class AuthService {
    constructor(
        private readonly usersService: UsersService,
        private readonly jwtService: JwtService,
        private readonly prisma: PrismaService,
        private readonly mailService: MailService,
    ) {}

    async register(registerDto: RegisterDto): Promise<AuthResponseDto> {
        const email = registerDto.email.trim().toLowerCase();

        const existingUser = await this.usersService.findByEmail(email);

        if (existingUser) {
            throw new BadRequestException('Email already registered');
        }

        const passwordHash = await bcrypt.hash(registerDto.password, 12);

        const user = await this.usersService.create({
            name: registerDto.name.trim(),
            email: registerDto.email,
            passwordHash,
        });

        const payload: JwtPayload = {
            sub: user.id,
            email: user.email,
        };

        const accessToken = await this.jwtService.signAsync(payload);

        return {
            accessToken: accessToken,
        } satisfies AuthResponseDto;
    }

    async login(dto: LoginDto) {
        const email = dto.email.trim().toLowerCase();
        const user = await this.usersService.findByEmail(email);

        if (!user) {
            throw new UnauthorizedException('Invalid email or password');
        }

        const passwordMatches = await bcrypt.compare(
            dto.password,
            user.passwordHash,
        );

        if (!passwordMatches) {
            throw new UnauthorizedException('Invalid email or password');
        }

        const payload: JwtPayload = {
            sub: user.id,
            email: user.email,
        };

        const accessToken = await this.jwtService.signAsync(payload);

        return {
            accessToken,
        } satisfies AuthResponseDto;
    }

    async changePassword(userId: string, currentPassword: string, newPassword: string) {
        const user = await this.prisma.user.findUnique({ where: { id: userId } });

        if (!user) {
            throw new UnauthorizedException('Account not found');
        }

        const matches = await bcrypt.compare(currentPassword, user.passwordHash);

        if (!matches) {
            throw new BadRequestException('Current password is incorrect');
        }

        const passwordHash = await bcrypt.hash(newPassword, 12);

        await this.prisma.user.update({
            where: { id: user.id },
            data: { passwordHash },
        });

        return { message: 'Password updated' };
    }

    async forgotPassword(emailRaw: string) {
        const email = emailRaw.trim().toLowerCase();

        const lastOtp = await this.prisma.passwordResetOtp.findFirst({
            where: { email },
            orderBy: { createdAt: 'desc' },
        });

        if (
            lastOtp &&
            Date.now() - lastOtp.createdAt.getTime() <
                AuthService.OTP_RESEND_COOLDOWN_MS
        ) {
            throw new HttpException(
                'Please wait 30 seconds before requesting a new code',
                HttpStatus.TOO_MANY_REQUESTS,
            );
        }

        const user = await this.usersService.findByEmail(email);

        if (!user) {
            return { message: 'If the email exists, a code has been sent' };
        }

        await this.prisma.passwordResetOtp.updateMany({
            where: { email, usedAt: null },
            data: { usedAt: new Date() },
        });

        const otp = crypto.randomInt(0, 1_000_000).toString().padStart(6, '0');
        const codeHash = await bcrypt.hash(otp, 10);

        await this.prisma.passwordResetOtp.create({
            data: {
                email,
                codeHash,
                expiresAt: new Date(Date.now() + AuthService.OTP_TTL_MS),
            },
        });

        await this.mailService.sendOtpEmail(email, otp);

        return { message: 'If the email exists, a code has been sent' };
    }

    async resetPassword(emailRaw: string, otp: string, newPassword: string) {
        const email = emailRaw.trim().toLowerCase();

        const record = await this.prisma.passwordResetOtp.findFirst({
            where: { email, usedAt: null },
            orderBy: { createdAt: 'desc' },
        });

        if (!record) {
            throw new BadRequestException('No active reset code for this email');
        }

        if (record.expiresAt.getTime() < Date.now()) {
            throw new BadRequestException('Reset code has expired');
        }

        if (record.attempts >= AuthService.OTP_MAX_ATTEMPTS) {
            throw new BadRequestException(
                'Too many attempts. Request a new code',
            );
        }

        const matches = await bcrypt.compare(otp, record.codeHash);

        if (!matches) {
            await this.prisma.passwordResetOtp.update({
                where: { id: record.id },
                data: { attempts: record.attempts + 1 },
            });
            throw new BadRequestException('Invalid reset code');
        }

        const user = await this.usersService.findByEmail(email);

        if (!user) {
            throw new BadRequestException('Account not found');
        }

        const passwordHash = await bcrypt.hash(newPassword, 12);

        await this.prisma.user.update({
            where: { id: user.id },
            data: { passwordHash },
        });

        await this.prisma.passwordResetOtp.update({
            where: { id: record.id },
            data: { usedAt: new Date() },
        });

        return { message: 'Password updated' };
    }

    private static readonly OTP_TTL_MS = 5 * 60 * 1000;
    private static readonly OTP_RESEND_COOLDOWN_MS = 30 * 1000;
    private static readonly OTP_MAX_ATTEMPTS = 5;
}
