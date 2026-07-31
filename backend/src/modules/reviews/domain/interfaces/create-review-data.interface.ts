import { ReviewResult } from '@prisma/client';

export interface CreateReviewData {
    studyItemId: string;

    result: ReviewResult;

    intervalDays: number;

    nextReviewAt: Date;
}
