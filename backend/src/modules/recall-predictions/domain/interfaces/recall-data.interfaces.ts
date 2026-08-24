export interface RecallReviewRow {
    studyItemId: string;
    isCorrect: boolean | null;
    confidenceScore: number | null;
    responseTimeMs: number | null;
    hesitationMs: number | null;
    answerChanges: number | null;
    createdAt: Date;
}

export interface DueQuestionRow {
    id: string;
    title: string | null;
    content: string | null;
    difficulty: string | null;
    nextReviewAt: Date | null;
    createdAt: Date;
    mediaDocumentId: string | null;
    options: string[] | null;

    topicName: string | null;
    subjectName: string | null;
}

export interface UserHistoryStats {
    totalReviews: number;

    correctCount: number;

    gradedCount: number;

    sumConfidence: number;

    confidenceCount: number;

    sumResponseTimeMs: number;

    responseTimeCount: number;

    sumHesitationMs: number;

    hesitationCount: number;
}
