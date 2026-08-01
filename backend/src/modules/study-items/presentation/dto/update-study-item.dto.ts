import { IsEnum, IsOptional, IsUUID } from 'class-validator';
import { Difficulty } from '@prisma/client';

export class UpdateStudyItemDto {
    @IsOptional()
    @IsEnum(Difficulty)
    difficulty?: Difficulty;

    @IsOptional()
    @IsUUID()
    topicId?: string;
}
