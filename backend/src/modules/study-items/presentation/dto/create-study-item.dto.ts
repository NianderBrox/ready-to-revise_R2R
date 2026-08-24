import {
    ArrayMaxSize,
    ArrayMinSize,
    IsArray,
    IsEnum,
    IsInt,
    IsOptional,
    IsString,
    IsUUID,
    Max,
    Min,
} from 'class-validator';

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

    @IsOptional()
    @IsArray()
    @ArrayMinSize(2)
    @ArrayMaxSize(6)
    @IsString({ each: true })
    options?: string[];

    @IsOptional()
    @IsInt()
    @Min(0)
    @Max(5)
    correctAnswerIndex?: number;
}
