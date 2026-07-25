import { IsEnum, IsUUID } from 'class-validator';

import { ReviewResult } from '@prisma/client';

export class CreateReviewDto {
    @IsUUID()
    studyItemId!: string;

    @IsEnum(ReviewResult)
    result!: ReviewResult;
}
