import { Module } from '@nestjs/common';
import { UsersService } from './application/services/users.service';

@Module({
    providers: [UsersService],
    exports: [UsersService],
})
export class UsersModule {}
