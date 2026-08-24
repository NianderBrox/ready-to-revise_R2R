export interface RecommendationItemDto {
    studyItemId: string;

    title: string | null;

    mediaDocumentId: string | null;

    options: string[] | null;

    expectedForgetDate: string | null;

    recallProbability: number | null;

    priority: 'high' | 'medium' | 'low' | null;

    rank: number;
}

export interface RecommendationItemMetaDto {
    restingNow: number;

    upcomingLater: number;
}

export interface RecommendationsResponseDto {
    source: 'ml' | 'scheduler';

    items: RecommendationItemDto[];

    meta?: RecommendationItemMetaDto;
}
