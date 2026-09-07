export interface RecommendationItemDto {
    studyItemId: string;

    title: string | null;

    mediaDocumentId: string | null;

    options: string[] | null;

    nextReviewAt: string | null;

    rank: number;
}

export interface RecommendationsResponseDto {
    source: 'scheduler';

    items: RecommendationItemDto[];
}
