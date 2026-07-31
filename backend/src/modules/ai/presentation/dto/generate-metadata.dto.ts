import { IsNotEmpty, IsString } from 'class-validator';

export class GenerateMetadataDto {
    @IsString()
    @IsNotEmpty()
    text!: string;
}
