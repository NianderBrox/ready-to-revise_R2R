import {
    BadRequestException,
    Injectable,
    UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { UsersService } from '../../../users/application/services/users.service';
import { JwtPayload } from '../../domain/interfaces/jwt-payload.interface';
import { AuthResponseDto } from '../../presentation/dto/auth-response.dto';
import { LoginDto } from '../../presentation/dto/login.dto';
import { RegisterDto } from '../../presentation/dto/register.dto';

@Injectable()
export class AuthService {
    constructor(
        private readonly usersService: UsersService,
        private readonly jwtService: JwtService,
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

        // const payload = {
        //   sub: user.id,
        //   email: user.email,
        // };

        const payload: JwtPayload = {
            sub: user.id,
            email: user.email,
        };

        const accessToken = await this.jwtService.signAsync(payload);

        return {
            accessToken,
        } satisfies AuthResponseDto;
    }
}
