import { ReviewResult } from '@prisma/client';

export class ReviewResponseDto {
    id!: string;

    studyItemId!: string;

    result!: ReviewResult;

    intervalDays!: number;

    reviewedAt!: Date;

    nextReviewAt!: Date;

    isCorrect?: boolean;

    confidenceScore?: number;

    createdAt!: Date;

    updatedAt!: Date;
}
