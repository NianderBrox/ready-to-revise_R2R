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

    recallProbability?: number;

    fsrsState?: number;

    fsrsStep?: number;

    fsrsStability?: number;

    fsrsDifficulty?: number;

    createdAt!: Date;

    updatedAt!: Date;
}
