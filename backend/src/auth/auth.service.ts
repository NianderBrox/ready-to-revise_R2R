import {
  BadRequestException,
  Injectable,
} from '@nestjs/common';
import * as bcrypt from 'bcrypt';

import { RegisterDto } from './dto/register.dto';
import { AuthResponseDto } from './dto/auth-response.dto';
import { UsersService } from '../users/users.service';

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
  ) {}

  async register(
    registerDto: RegisterDto,
  ): Promise<AuthResponseDto> {
    const existingUser =
      await this.usersService.findByEmail(
        registerDto.email,
      );

    if (existingUser) {
      throw new BadRequestException(
        'Email already registered',
      );
    }

    const passwordHash = await bcrypt.hash(
      registerDto.password,
      12,
    );

    const user = await this.usersService.create({
      name: registerDto.name,
      email: registerDto.email,
      passwordHash,
    });

    return {
      id: user.id,
      name: user.name,
      email: user.email,
    };
  }
}