import { IsEnum, IsOptional, IsString, IsUUID } from 'class-validator';

import { Difficulty, StudyItemType } from '../../../../common/enums';

export class CreateStudyItemDto {
    @IsOptional()
    @IsString()
    title?: string;

    @IsOptional()
    @IsString()
    content?: string;

    @IsEnum(StudyItemType)
    type!: StudyItemType;

    @IsOptional()
    @IsEnum(Difficulty)
    difficulty?: Difficulty;

    @IsOptional()
    @IsUUID()
    topicId?: string;
}
