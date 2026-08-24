import { ReviewResult } from '@prisma/client';

type GradeKey = Exclude<ReviewResult, 'AGAIN' | 'MEMORIZED'>;

export class ReviewScheduler {
    private static readonly BASE_INTERVAL_DAYS: Record<GradeKey, number> = {
        HARD: 3,
        GOOD: 7,
        EASY: 14,
    };

    private static readonly EXPANSION_MULTIPLIERS: Record<GradeKey, number> = {
        HARD: 1.2,
        GOOD: 1.9,
        EASY: 2.6,
    };

    private static readonly MEMORY_MULTIPLIER = 1.5;

    private static readonly MAX_INTERVAL_DAYS = 365;

    private static readonly AGAIN_INTERVAL_DAYS = 1;

    static calculate(
        result: ReviewResult,
        previousIntervalDays?: number | null,
    ): number {
        if (result === ReviewResult.AGAIN) {
            return ReviewScheduler.AGAIN_INTERVAL_DAYS;
        }

        if (result === ReviewResult.MEMORIZED) {
            const goodInterval = ReviewScheduler.progressGood(
                previousIntervalDays,
            );

            return Math.min(
                ReviewScheduler.MAX_INTERVAL_DAYS,
                Math.ceil(goodInterval * ReviewScheduler.MEMORY_MULTIPLIER),
            );
        }

        const base = ReviewScheduler.BASE_INTERVAL_DAYS[result];

        if (
            previousIntervalDays === undefined ||
            previousIntervalDays === null ||
            previousIntervalDays <= 0
        ) {
            return base;
        }

        const multiplier = ReviewScheduler.EXPANSION_MULTIPLIERS[result];

        const expanded = Math.ceil(previousIntervalDays * multiplier);

        return Math.min(
            ReviewScheduler.MAX_INTERVAL_DAYS,
            Math.max(base, expanded),
        );
    }

    static nextReviewDate(intervalDays: number): Date {
        const next = new Date();

        next.setDate(next.getDate() + intervalDays);

        return next;
    }

    private static progressGood(
        previousIntervalDays?: number | null,
    ): number {
        const base = ReviewScheduler.BASE_INTERVAL_DAYS.GOOD;

        if (
            previousIntervalDays === undefined ||
            previousIntervalDays === null ||
            previousIntervalDays <= 0
        ) {
            return base;
        }

        const expanded = Math.ceil(
            previousIntervalDays *
                ReviewScheduler.EXPANSION_MULTIPLIERS.GOOD,
        );

        return Math.max(base, expanded);
    }
}
