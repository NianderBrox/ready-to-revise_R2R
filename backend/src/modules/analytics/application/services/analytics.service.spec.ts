import { AnalyticsService } from './analytics.service';
import { AnalyticsRepository } from '../../infrastructure/repositories/analytics.repository';

type RepoMock = {
    getTodayCounts: jest.Mock;
    getAccuracyCounts: jest.Mock;
    getReviewDates: jest.Mock;
    getRangedReviews: jest.Mock;
    getSubjectRows: jest.Mock;
    getRecent: jest.Mock;
    getYearActivity: jest.Mock;
};

export function buildRepo(overrides: Partial<RepoMock> = {}): RepoMock {
    return {
        getTodayCounts: jest.fn().mockResolvedValue({
            studyItems: 0,
            dueToday: 0,
            completedToday: 0,
        }),
        getAccuracyCounts: jest
            .fn()
            .mockResolvedValue({ total: 0, correct: 0, incorrect: 0 }),
        getReviewDates: jest.fn().mockResolvedValue([]),
        getRangedReviews: jest.fn().mockResolvedValue([]),
        getSubjectRows: jest.fn().mockResolvedValue([]),
        getRecent: jest.fn().mockResolvedValue([]),
        getYearActivity: jest.fn().mockResolvedValue([]),
        ...overrides,
    };
}

function makeReview(overrides: {
    reviewedAt: Date;
    isCorrect?: boolean | null;
    responseTimeMs?: number | null;
    sessionDurationMinutes?: number | null;
    sessionId?: string | null;
}) {
    return {
        isCorrect: null,
        responseTimeMs: null,
        sessionDurationMinutes: null,
        sessionId: null,
        ...overrides,
    };
}

describe('AnalyticsService', () => {
    let repo: RepoMock;
    let service: AnalyticsService;

    beforeEach(() => {
        repo = buildRepo();
        service = new AnalyticsService(repo as unknown as AnalyticsRepository);
    });

    describe('getOverview', () => {
        it('default range is 7d and returns empty-safe series of 7 UTC days', async () => {
            const result = await service.getOverview('user-1');

            expect(result.range).toBe('7d');
            expect(result.daily).toHaveLength(7);
            expect(result.summary).toEqual({
                totalReviews: 0,
                accuracyPct: 0,
                currentStreak: 0,
                longestStreak: 0,
                completionToday: 0,
                studyItems: 0,
                dueToday: 0,
            });
            expect(result.subjects).toEqual([]);
            expect(result.recent).toEqual([]);
        });

        it('range=30d returns 30 daily buckets', async () => {
            const result = await service.getOverview('user-1', '30d');
            expect(result.range).toBe('30d');
            expect(result.daily).toHaveLength(30);
        });

        it('unknown range falls back to 7d', async () => {
            const result = await service.getOverview('user-1', 'banana');
            expect(result.range).toBe('7d');
            expect(result.daily).toHaveLength(7);
        });

        it('summary accuracy is rounded from correct/(correct+incorrect)', async () => {
            repo.getAccuracyCounts.mockResolvedValue({
                total: 5,
                correct: 2,
                incorrect: 2,
            });

            const result = await service.getOverview('user-1', '7d');

            expect(result.summary.accuracyPct).toBe(50);
            expect(repo.getAccuracyCounts).toHaveBeenCalledWith(
                'user-1',
                expect.any(Date),
            );
        });

        it('completionToday follows done/(done+due)', async () => {
            repo.getTodayCounts.mockResolvedValue({
                studyItems: 10,
                dueToday: 6,
                completedToday: 2,
            });

            const result = await service.getOverview('user-1', '7d');

            expect(result.summary.completionToday).toBe(25);
        });

        it('totalReviews is the count of ranged reviews', async () => {
            repo.getRangedReviews.mockResolvedValue([
                makeReview({ reviewedAt: new Date('2026-10-17T08:00:00.000Z') }),
                makeReview({ reviewedAt: new Date('2026-10-17T09:00:00.000Z') }),
            ]);

            const result = await service.getOverview('user-1', '7d');

            expect(result.summary.totalReviews).toBe(2);
        });

        it('maps recent reviews with study item title', async () => {
            repo.getRecent.mockResolvedValue([
                {
                    reviewedAt: new Date('2026-10-17T08:00:00.000Z'),
                    isCorrect: true,
                    studyItem: {
                        title: 'What is photosynthesis?',
                        content: null,
                        type: 'QUESTION',
                    },
                },
            ]);

            const result = await service.getOverview('user-1', '7d');

            expect(result.recent).toEqual([
                {
                    title: 'What is photosynthesis?',
                    reviewedAt: new Date('2026-10-17T08:00:00.000Z'),
                    isCorrect: true,
                },
            ]);
        });
    });

    describe('buildDailySeries', () => {
        it('buckets reviews per UTC day and excludes null accuracy from accuracyPct', async () => {
            const service = new AnalyticsService(repo as unknown as AnalyticsRepository);
            const rangeStart = new Date('2026-10-11T00:00:00.000Z');

            const daily = service.buildDailySeries(
                [
                    makeReview({
                        reviewedAt: new Date('2026-10-13T10:00:00.000Z'),
                        isCorrect: true,
                    }),
                    makeReview({
                        reviewedAt: new Date('2026-10-13T11:00:00.000Z'),
                        isCorrect: false,
                    }),
                    makeReview({
                        reviewedAt: new Date('2026-10-13T12:00:00.000Z'),
                        isCorrect: null,
                    }),
                ],
                rangeStart,
                7,
            );

            const oct13 = daily.find((point) => point.date === '2026-10-13');
            const oct12 = daily.find((point) => point.date === '2026-10-12');

            expect(daily).toHaveLength(7);
            expect(oct12?.completed).toBe(0);
            expect(oct13?.completed).toBe(3);
            expect(oct13?.accuracyPct).toBe(50);
        });

        it('counts session minutes once per sessionId and uses responseTime fallback', () => {
            const service = new AnalyticsService(repo as unknown as AnalyticsRepository);
            const rangeStart = new Date('2026-10-11T00:00:00.000Z');

            const daily = service.buildDailySeries(
                [
                    makeReview({
                        reviewedAt: new Date('2026-10-13T10:00:00.000Z'),
                        sessionId: 'session-1',
                        sessionDurationMinutes: 12,
                    }),
                    makeReview({
                        reviewedAt: new Date('2026-10-13T10:30:00.000Z'),
                        sessionId: 'session-1',
                        sessionDurationMinutes: 12,
                    }),
                    makeReview({
                        reviewedAt: new Date('2026-10-14T09:00:00.000Z'),
                        responseTimeMs: 90000,
                    }),
                    makeReview({
                        reviewedAt: new Date('2026-10-14T21:00:00.000Z'),
                    }),
                ],
                rangeStart,
                7,
            );

            const oct13 = daily.find((point) => point.date === '2026-10-13');
            const oct14 = daily.find((point) => point.date === '2026-10-14');

            expect(oct13?.minutes).toBe(12);
            expect(oct14?.minutes).toBe(2);
        });
    });

    describe('streaks', () => {
        it('currentStreak counts any-review consecutive days ending today', () => {
            const service = new AnalyticsService(repo as unknown as AnalyticsRepository);

            const dates = [
                '2026-10-15',
                '2026-10-16',
                '2026-10-17',
            ];

            expect(service.currentStreak(dates, '2026-10-17')).toBe(3);
        });
        it('currentStreak counts a yesterday-ending streak as alive', () => {
            const service = new AnalyticsService(repo as unknown as AnalyticsRepository);

            expect(
                service.currentStreak(['2026-10-15', '2026-10-16'], '2026-10-17'),
            ).toBe(2);
        });

        it('currentStreak breaks when a day is missed mid-run', () => {
            const service = new AnalyticsService(repo as unknown as AnalyticsRepository);

            expect(
                service.currentStreak(
                    ['2026-10-14', '2026-10-15', '2026-10-17'],
                    '2026-10-17',
                ),
            ).toBe(1);
        });

        it('longestStreak finds the longest run, not necessarily recent', () => {
            const service = new AnalyticsService(repo as unknown as AnalyticsRepository);

            expect(
                service.longestStreak([
                    '2026-09-01',
                    '2026-09-02',
                    '2026-09-03',
                    '2026-10-16',
                    '2026-10-17',
                ]),
            ).toBe(3);
        });
    });

    describe('getHeatmap', () => {
        function dayFor(date: string, count: number) {
            return { d: new Date(`${date}T12:00:00.000Z`), c: count };
        }

        it('returns 365 days ending today with levels by threshold ladder', async () => {
            repo.getYearActivity.mockResolvedValue([
                dayFor('2026-10-17', 10),
                dayFor('2026-10-16', 9),
                dayFor('2026-10-15', 6),
                dayFor('2026-10-14', 5),
                dayFor('2026-10-13', 2),
                dayFor('2026-10-12', 1),
            ]);

            const result = await service.getHeatmap('user-1');

            expect(result.days).toHaveLength(365);
            const byDate = new Map(result.days.map((d) => [d.date, d.level]));
            expect(byDate.get('2026-10-17')).toBe(4);
            expect(byDate.get('2026-10-16')).toBe(3);
            expect(byDate.get('2026-10-15')).toBe(3);
            expect(byDate.get('2026-10-14')).toBe(2);
            expect(byDate.get('2026-10-13')).toBe(1);
            expect(byDate.get('2026-10-12')).toBe(1);
            const active = result.days.filter((d) => d.level > 0);
            expect(active).toHaveLength(6);
        });

        it('empty-user heatmap is all zeros at level 0 with exact bounds', async () => {
            const result = await service.getHeatmap('user-2');

            expect(result.days).toHaveLength(365);
            expect(result.days.every((d) => d.completed === 0 && d.level === 0)).toBe(true);
            const today = new Date().toISOString().slice(0, 10);
            expect(result.days[364].date).toBe(today);
        });
    });
});
