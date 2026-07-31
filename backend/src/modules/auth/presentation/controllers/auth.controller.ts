import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { Get, UseGuards } from '@nestjs/common';
import { AuthService } from '../../application/services/auth.service';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { JwtAuthGuard } from '../../infrastructure/guards/jwt-auth.guard';
import { AuthResponseDto } from '../dto/auth-response.dto';
import { LoginDto } from '../dto/login.dto';
import { RegisterDto } from '../dto/register.dto';

@Controller('auth')
export class AuthController {
    constructor(private readonly authService: AuthService) {}

    @Post('register')
    @HttpCode(HttpStatus.CREATED)
    register(@Body() dto: RegisterDto): Promise<AuthResponseDto> {
        return this.authService.register(dto);
    }

    @Post('login')
    login(@Body() dto: LoginDto) {
        return this.authService.login(dto);
    }

    @Get('profile')
    @UseGuards(JwtAuthGuard)
    getProfile(@CurrentUser() user: CurrentUserData) {
        return user;
    }
}
