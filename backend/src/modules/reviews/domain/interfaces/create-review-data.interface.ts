import { ReviewResult } from '@prisma/client';

export interface CreateReviewData {
    studyItemId: string;

    result: ReviewResult;

    intervalDays: number;

    nextReviewAt: Date;

    selectedOptionIndex: number | null;

    isCorrect: boolean | null;

    confidenceScore?: number | null;

    responseTimeMs?: number | null;

    hesitationMs?: number | null;

    answerChanges?: number | null;

    sessionId?: string | null;
}
