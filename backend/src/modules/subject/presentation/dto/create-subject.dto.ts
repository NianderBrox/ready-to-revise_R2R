import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class CreateSubjectDto {
    @IsString()
    @IsNotEmpty()
    @MaxLength(100)
    name!: string;
}
