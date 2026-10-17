import { Injectable } from '@nestjs/common';
import {
    AnalyticsHeatmapResponseDto,
    AnalyticsOverviewDto,
} from '../../presentation/dto/analytics-overview.dto';
import { AnalyticsRepository } from '../../infrastructure/repositories/analytics.repository';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

const HEATMAP_DAYS = 365;

const UTC_DAYS = 'YYYY-MM-DD';

export type AnalyticsRange = '7d' | '30d';

function parseRange(value?: string): AnalyticsRange {
    return value === '30d' ? '30d' : '7d';
}

function toUtcDateKey(date: Date): string {
    return date.toISOString().slice(0, 10);
}

function utcStartOfDay(date: Date): Date {
    const key = toUtcDateKey(date);
    return new Date(`${key}T00:00:00.000Z`);
}

@Injectable()
export class AnalyticsService {
    constructor(private readonly repository: AnalyticsRepository) {}

    async getOverview(
        userId: string,
        rangeValue?: string,
    ): Promise<AnalyticsOverviewDto> {
        const range = parseRange(rangeValue);
        const days = range === '30d' ? 30 : 7;

        const now = new Date();
        const todayKey = toUtcDateKey(now);
        const rangeStart = new Date(
            utcStartOfDay(now).getTime() - (days - 1) * MS_PER_DAY,
        );

        const [
            todayCounts,
            accuracyCounts,
            reviewDates,
            rangedReviews,
            subjectRows,
            recentRows,
        ] = await Promise.all([
            this.repository.getTodayCounts(userId, now),
            this.repository.getAccuracyCounts(userId, rangeStart),
            this.repository.getReviewDates(userId),
            this.repository.getRangedReviews(userId, rangeStart),
            this.repository.getSubjectRows(userId, rangeStart, now),
            this.repository.getRecent(userId, 10),
        ]);

        const graded = accuracyCounts.correct + accuracyCounts.incorrect;

        const daily = this.buildDailySeries(rangedReviews, rangeStart, days);

        const completedToday = todayCounts.completedToday;
        const completionToday =
            completedToday + todayCounts.dueToday > 0
                ? Math.round(
                      (100 * completedToday) /
                          (completedToday + todayCounts.dueToday),
                  )
                : 0;

        return {
            range,
            summary: {
                totalReviews: rangedReviews.length,
                accuracyPct: graded > 0 ? Math.round((100 * accuracyCounts.correct) / graded) : 0,
                currentStreak: this.currentStreak(reviewDates, todayKey),
                longestStreak: this.longestStreak(reviewDates),
                completionToday,
                studyItems: todayCounts.studyItems,
                dueToday: todayCounts.dueToday,
            },
            daily,
            subjects: subjectRows,
            recent: recentRows.map((row) => ({
                title: row.studyItem.title ?? '',
                reviewedAt: row.reviewedAt,
                isCorrect: row.isCorrect,
            })),
        };
    }

    buildDailySeries(
        reviews: AnalysableReview[],
        rangeStart: Date,
        days: number,
    ): AnalyticsOverviewDto['daily'] {
        const buckets = new Map<
            string,
            { completed: number; correct: number; incorrect: number; minutes: number }
        >();

        for (let i = 0; i < days; i += 1) {
            const key = toUtcDateKey(
                new Date(rangeStart.getTime() + i * MS_PER_DAY),
            );
            buckets.set(key, {
                completed: 0,
                correct: 0,
                incorrect: 0,
                minutes: 0,
            });
        }

        const seenSessions = new Set<string>();

        for (const review of reviews) {
            const key = toUtcDateKey(review.reviewedAt);
            const bucket = buckets.get(key);
            if (!bucket) {
                continue;
            }
            bucket.completed += 1;
            if (review.isCorrect === true) {
                bucket.correct += 1;
            } else if (review.isCorrect === false) {
                bucket.incorrect += 1;
            }

            if (review.sessionId !== null) {
                if (seenSessions.has(review.sessionId)) {
                    continue;
                }
                seenSessions.add(review.sessionId);
                bucket.minutes += Math.max(0, review.sessionDurationMinutes ?? 0);
            } else {
                bucket.minutes += Math.max(0, review.responseTimeMs ?? 0) / 60000;
            }
        }

        return Array.from(buckets.entries()).map(([date, bucket]) => {
            const graded = bucket.correct + bucket.incorrect;
            return {
                date,
                completed: bucket.completed,
                accuracyPct: graded > 0 ? Math.round((100 * bucket.correct) / graded) : 0,
                minutes: Math.round(bucket.minutes),
            };
        });
    }

    currentStreak(dates: string[], todayKey: string): number {
        const unique = new Set(dates);

        let cursor = new Date(`${todayKey}T00:00:00.000Z`).getTime();

        if (!unique.has(toUtcDateKey(new Date(cursor)))) {
            cursor -= MS_PER_DAY;
            if (!unique.has(toUtcDateKey(new Date(cursor)))) {
                return 0;
            }
        }

        let streak = 0;
        while (unique.has(toUtcDateKey(new Date(cursor)))) {
            streak += 1;
            cursor -= MS_PER_DAY;
        }

        return streak;
    }

    longestStreak(dates: string[]): number {
        const sorted = Array.from(new Set(dates)).sort();
        let longest = 0;
        let run = 0;
        let previous = '';

        for (const date of sorted) {
            if (previous === '') {
                run = 1;
            } else {
                const previousMs = Date.parse(`${previous}T00:00:00.000Z`);
                const dateMs = Date.parse(`${date}T00:00:00.000Z`);
                run = dateMs - previousMs === MS_PER_DAY ? run + 1 : 1;
            }
            if (run > longest) {
                longest = run;
            }
            previous = date;
        }

        return longest;
    }

    async getHeatmap(userId: string): Promise<AnalyticsHeatmapResponseDto> {
        const now = new Date();
        const todayKey = toUtcDateKey(now);
        const startDate = new Date(
            utcStartOfDay(now).getTime() - (HEATMAP_DAYS - 1) * MS_PER_DAY,
        );

        const counts = new Map<string, number>();

        for (const row of await this.repository.getYearActivity(userId, startDate)) {
            const key = toUtcDateKey(
                row.d instanceof Date ? row.d : new Date(String(row.d)),
            );
            counts.set(key, Number(row.c));
        }

        const days: AnalyticsHeatmapResponseDto['days'] = [];
        for (let i = 0; i < HEATMAP_DAYS; i += 1) {
            const date = new Date(startDate.getTime() + i * MS_PER_DAY);
            const key = toUtcDateKey(date);
            const completed = counts.get(key) ?? 0;
            days.push({
                date: key,
                completed,
                level: this.levelOf(completed),
            });
        }

        return {
            startDate: toUtcDateKey(startDate),
            days,
        } satisfies AnalyticsHeatmapResponseDto;
    }

    private levelOf(count: number): number {
        if (count <= 0) {
            return 0;
        }
        if (count <= 2) {
            return 1;
        }
        if (count <= 5) {
            return 2;
        }
        if (count <= 9) {
            return 3;
        }
        return 4;
    }
}

export interface AnalysableReview {
    reviewedAt: Date;
    isCorrect: boolean | null;
    responseTimeMs: number | null;
    sessionDurationMinutes: number | null;
    sessionId: string | null;
}
