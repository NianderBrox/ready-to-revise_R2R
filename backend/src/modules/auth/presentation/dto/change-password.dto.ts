import { IsNotEmpty, MinLength } from 'class-validator';

export class ChangePasswordDto {
    @IsNotEmpty()
    currentPassword!: string;

    @MinLength(8)
    newPassword!: string;
}
