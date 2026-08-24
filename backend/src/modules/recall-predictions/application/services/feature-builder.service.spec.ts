import { FeatureBuilderService } from './feature-builder.service';
import {
    DueQuestionRow,
    RecallReviewRow,
    UserHistoryStats,
} from '../../domain/interfaces/recall-data.interfaces';

const NOW = new Date('2026-08-22T14:00:00.000Z');

const EMPTY_STATS: UserHistoryStats = {
    totalReviews: 0,
    correctCount: 0,
    gradedCount: 0,
    sumConfidence: 0,
    confidenceCount: 0,
    sumResponseTimeMs: 0,
    responseTimeCount: 0,
    sumHesitationMs: 0,
    hesitationCount: 0,
};

function makeItem(overrides: Partial<DueQuestionRow> = {}): DueQuestionRow {
    return {
        id: 'item-1',
        title: 'What is photosynthesis?',
        content: null,
        difficulty: 'MEDIUM',
        nextReviewAt: new Date('2026-08-21T12:00:00.000Z'),
        createdAt: new Date('2026-08-13T12:00:00.000Z'),
        options: ['A', 'B', 'C', 'D'],
        topicName: 'Biology',
        subjectName: 'Science',
        ...overrides,
    };
}

function makeReview(overrides: Partial<RecallReviewRow> = {}): RecallReviewRow {
    return {
        studyItemId: 'item-1',
        isCorrect: true,
        confidenceScore: 0.8,
        responseTimeMs: 10000,
        hesitationMs: 2000,
        answerChanges: 1,
        createdAt: new Date('2026-08-20T12:00:00.000Z'),
        ...overrides,
    };
}

describe('FeatureBuilderService', () => {
    let service: FeatureBuilderService;

    beforeEach(() => {
        service = new FeatureBuilderService();
    });

    it('maps difficulty enums to contract scores with MEDIUM default', () => {
        const easy = service.build({
            item: makeItem({ difficulty: 'EASY' }),
            reviews: [],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        }).difficulty;

        const hard = service.build({
            item: makeItem({ difficulty: 'HARD' }),
            reviews: [],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        }).difficulty;

        const unknown = service.build({
            item: makeItem({ difficulty: null }),
            reviews: [],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        }).difficulty;

        expect(easy).toBe(1);
        expect(hard).toBe(3);
        expect(unknown).toBe(2);
    });

    it('converts millisecond telemetry to seconds and averages history', () => {
        const stats: UserHistoryStats = {
            ...EMPTY_STATS,
            sumResponseTimeMs: 30000,
            responseTimeCount: 3,
            sumHesitationMs: 6000,
            hesitationCount: 3,
        };

        const features = service.build({
            item: makeItem(),
            reviews: [],
            userStats: stats,
            questionGlobalRate: null,
            now: NOW,
        });

        expect(features.average_response_time).toBeCloseTo(10, 5);
        expect(features.average_hesitation).toBeCloseTo(2, 5);
        expect(features.normalized_avg_response_time).toBeCloseTo(10 / 120, 6);
        expect(features.normalized_avg_hesitation).toBeCloseTo(2 / 30, 6);
    });

    it('derives interval and power-law retrievability anchored at 0.9', () => {
        const lastReview = makeReview({
            createdAt: new Date('2026-08-13T12:00:00.000Z'),
        });

        const features = service.build({
            item: makeItem(),
            reviews: [lastReview],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        });

        const elapsedDays =
            (NOW.getTime() - lastReview.createdAt.getTime()) / 86400000;

        expect(features.review_interval_days).toBeCloseTo(8, 4);

        expect(features.fsrs_recall_probability).toBeCloseTo(
            Math.pow(0.9, elapsedDays / 8),
            5,
        );

        expect(features.had_fsrs_estimate).toBe(true);
    });

    it('returns null fsrs estimate without history or interval', () => {
        const noHistory = service.build({
            item: makeItem({ nextReviewAt: null }),
            reviews: [],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        });

        expect(noHistory.fsrs_recall_probability).toBeNull();

        expect(noHistory.had_fsrs_estimate).toBe(false);
    });

    it('counts consecutive correct streaks from the most recent review', () => {
        const reviews = [
            makeReview({ isCorrect: true }),
            makeReview({ isCorrect: true, createdAt: new Date('2026-08-19') }),
            makeReview({ isCorrect: false, createdAt: new Date('2026-08-18') }),
        ];

        const features = service.build({
            item: makeItem(),
            reviews,
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        });

        expect(features.consecutive_correct).toBe(2);
    });

    it('computes recent window rates over the last five reviews only', () => {
        const reviews = [
            makeReview({ isCorrect: false }),
            makeReview({ isCorrect: true }),
            makeReview({ isCorrect: true }),
            makeReview({ isCorrect: true }),
            makeReview({ isCorrect: true }),
            makeReview({ isCorrect: false }),
        ];

        const features = service.build({
            item: makeItem(),
            reviews,
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        });

        expect(features.recent_success_rate_5).toBeCloseTo(0.8, 6);
        expect(features.total_revisions).toBe(6);
    });

    it('uses python-style day_of_week (monday=0) and clock hour', () => {
        const mondayNoon = new Date('2026-08-24T09:30:00.000Z');

        const features = service.build({
            item: makeItem(),
            reviews: [],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: mondayNoon,
        });

        expect(features.day_of_week).toBe(0);
        expect(features.hour_of_day).toBe(mondayNoon.getHours());
    });

    it('passes names through and counts words/characters of the prompt', () => {
        const features = service.build({
            item: makeItem(),
            reviews: [],
            userStats: EMPTY_STATS,
            questionGlobalRate: null,
            now: NOW,
        });

        expect(features.subject).toBe('Science');
        expect(features.topic).toBe('Biology');
        expect(features.word_count).toBe(3);
        expect(features.character_count).toBe('What is photosynthesis?'.length);
    });
});
