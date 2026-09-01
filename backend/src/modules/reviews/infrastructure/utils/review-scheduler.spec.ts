import { ReviewResult } from '@prisma/client';
import { ReviewScheduler } from './review-scheduler';

describe('ReviewScheduler', () => {
    describe('first review (no previous interval)', () => {
        it.each([
            [ReviewResult.AGAIN, 1],
            [ReviewResult.HARD, 3],
            [ReviewResult.GOOD, 7],
            [ReviewResult.EASY, 14],
        ])('returns base interval for %s', (result, expected) => {
            expect(ReviewScheduler.calculate(result)).toBe(expected);
        });

        it('treats zero or negative previous intervals as first review', () => {
            expect(ReviewScheduler.calculate(ReviewResult.GOOD, 0)).toBe(7);

            expect(ReviewScheduler.calculate(ReviewResult.EASY, -3)).toBe(14);
        });
    });

    describe('expanding intervals on success', () => {
        it('grows a GOOD streak: 7 → 14 → 27 → 52', () => {
            expect(ReviewScheduler.calculate(ReviewResult.GOOD, 7)).toBe(14);

            expect(ReviewScheduler.calculate(ReviewResult.GOOD, 14)).toBe(27);

            expect(ReviewScheduler.calculate(ReviewResult.GOOD, 27)).toBe(52);
        });

        it('grows an EASY streak faster than GOOD', () => {
            const easy = ReviewScheduler.calculate(ReviewResult.EASY, 14);

            const good = ReviewScheduler.calculate(ReviewResult.GOOD, 14);

            expect(easy).toBeGreaterThan(good);
        });

        it('HARD grows slowly and never below its base', () => {
            expect(ReviewScheduler.calculate(ReviewResult.HARD, 1)).toBe(3);

            expect(ReviewScheduler.calculate(ReviewResult.HARD, 3)).toBe(4);
        });
    });

    describe('lapse handling', () => {
        it('resets to one day after AGAIN regardless of history', () => {
            expect(ReviewScheduler.calculate(ReviewResult.AGAIN, 180)).toBe(1);
        });

        it('recovers to at least the grade base after a lapse', () => {
            expect(ReviewScheduler.calculate(ReviewResult.GOOD, 1)).toBe(7);
        });
    });

    describe('memorized self-grade (D12)', () => {
        it('first memorize applies the memory multiplier to the GOOD base: 7 → 11', () => {
            expect(ReviewScheduler.calculate(ReviewResult.MEMORIZED)).toBe(11);

            expect(ReviewScheduler.calculate(ReviewResult.MEMORIZED, 0)).toBe(
                11,
            );
        });

        it('memorize after a GOOD streak compounds expansion then multiplies: 7 → 21, 14 → 41', () => {
            expect(ReviewScheduler.calculate(ReviewResult.MEMORIZED, 7)).toBe(
                21,
            );

            expect(ReviewScheduler.calculate(ReviewResult.MEMORIZED, 14)).toBe(
                41,
            );
        });

        it('always beats a plain GOOD on identical history', () => {
            const prev = 30;

            expect(
                ReviewScheduler.calculate(ReviewResult.MEMORIZED, prev),
            ).toBeGreaterThan(
                ReviewScheduler.calculate(ReviewResult.GOOD, prev),
            );
        });

        it('never exceeds the cap even when multiplied', () => {
            expect(
                ReviewScheduler.calculate(ReviewResult.MEMORIZED, 365),
            ).toBeLessThanOrEqual(365);
        });
    });

    describe('cap', () => {
        it('never exceeds 365 days', () => {
            expect(
                ReviewScheduler.calculate(ReviewResult.EASY, 365),
            ).toBeLessThanOrEqual(365);
        });
    });

    describe('nextReviewDate', () => {
        it('adds the interval in days to now', () => {
            const before = new Date();

            const next = ReviewScheduler.nextReviewDate(5);

            const after = new Date();

            const ms = next.getTime();

            expect(ms).toBeGreaterThanOrEqual(
                before.getTime() + 5 * 24 * 60 * 60 * 1000,
            );

            expect(ms).toBeLessThanOrEqual(
                after.getTime() + 5 * 24 * 60 * 60 * 1000,
            );
        });
    });
});
