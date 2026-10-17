export class AnalyticsSummaryDto {
    totalReviews!: number;

    accuracyPct!: number;

    currentStreak!: number;

    longestStreak!: number;

    completionToday!: number;

    studyItems!: number;

    dueToday!: number;
}

export class AnalyticsDailyPointDto {
    date!: string;

    completed!: number;

    accuracyPct!: number;

    minutes!: number;
}

export class AnalyticsSubjectRowDto {
    name!: string;

    items!: number;

    due!: number;

    reviews!: number;

    accuracyPct!: number;
}

export class AnalyticsRecentRowDto {
    title!: string;

    reviewedAt!: Date;

    isCorrect!: boolean | null;
}

export class AnalyticsHeatmapDayDto {
    date!: string;

    completed!: number;

    level!: number;
}

export class AnalyticsHeatmapResponseDto {
    startDate!: string;

    days!: AnalyticsHeatmapDayDto[];
}

export class AnalyticsOverviewDto {
    range!: string;

    summary!: AnalyticsSummaryDto;

    daily!: AnalyticsDailyPointDto[];

    subjects!: AnalyticsSubjectRowDto[];

    recent!: AnalyticsRecentRowDto[];
}
