import { IsEmail, IsNotEmpty, Matches, MinLength } from 'class-validator';

export class ResetPasswordDto {
    @IsEmail()
    email!: string;

    @Matches(/^\d{6}$/, { message: 'OTP must be a 6-digit number' })
    otp!: string;

    @MinLength(8)
    newPassword!: string;
}
