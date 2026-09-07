import { ReviewResult } from '@prisma/client';

export interface CreateReviewData {
    studyItemId: string;

    result: ReviewResult;

    intervalDays: number;

    nextReviewAt: Date;

    recallProbability?: number | null;

    fsrsState?: number | null;

    fsrsStep?: number | null;

    fsrsStability?: number | null;

    fsrsDifficulty?: number | null;

    selectedOptionIndex: number | null;

    isCorrect: boolean | null;

    confidenceScore?: number | null;

    responseTimeMs?: number | null;

    hesitationMs?: number | null;

    answerChanges?: number | null;

    sessionId?: string | null;

    sessionDurationMinutes?: number | null;

    questionPositionInSession?: number | null;
}
