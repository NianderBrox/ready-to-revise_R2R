import { RecommendationsService } from './recommendations.service';
import { FeatureBuilderService } from './feature-builder.service';
import { RecallQueryRepository } from '../../infrastructure/repositories/recall-query.repository';
import { MlHttpService } from '../../../ml-client/infrastructure/http/ml-http.service';
import { MlConfigService } from '../../../ml-client/application/services/ml-config.service';
import { ConfigService } from '@nestjs/config';
import {
    DueQuestionRow,
    RecallReviewRow,
} from '../../domain/interfaces/recall-data.interfaces';

const HOUR = 60 * 60 * 1000;

const DAY = 24 * HOUR;

function makeItem(overrides: Partial<DueQuestionRow> = {}): DueQuestionRow {
    return {
        id: 'item-1',
        title: 'Question A',
        content: null,
        difficulty: 'MEDIUM',
        nextReviewAt: new Date(Date.now() - 2 * DAY),
        createdAt: new Date(Date.now() - 3 * DAY),
        options: ['A', 'B', 'C', 'D'],
        topicName: 'T',
        subjectName: 'S',
        ...overrides,
    };
}

function makeReview(overrides: Partial<RecallReviewRow> = {}): RecallReviewRow {
    return {
        studyItemId: 'item-1',
        isCorrect: true,
        confidenceScore: 0.8,
        responseTimeMs: 10000,
        hesitationMs: 500,
        answerChanges: 0,
        createdAt: new Date(Date.now() - 2 * DAY),
        ...overrides,
    };
}

function setup(
    repoOverride: {
        findCandidateQuestions?: jest.Mock;
        findReviewsForItems?: jest.Mock;
        findUserReviewRows?: jest.Mock;
    } = {},
    mlOverride: {
        isAvailable?: boolean;
        recommend?: jest.Mock;
    } = {},
) {
    const findCandidateQuestions =
        repoOverride.findCandidateQuestions ?? jest.fn().mockResolvedValue([]);

    const findReviewsForItems =
        repoOverride.findReviewsForItems ?? jest.fn().mockResolvedValue([]);

    const findUserReviewRows =
        repoOverride.findUserReviewRows ?? jest.fn().mockResolvedValue([]);

    const repository = {
        findDueQuestions: jest.fn(),
        findCandidateQuestions,
        findReviewsForItems,
        findUserReviewRows,
    } as unknown as RecallQueryRepository;

    const recommend = mlOverride.recommend ?? jest.fn();

    const ml = {
        isAvailable: mlOverride.isAvailable ?? true,
        recommend,
        markUnavailable: jest.fn(),
    } as unknown as MlHttpService;

    const mlConfig = {
        modelName: 'gradient_boosting',
    } as unknown as MlConfigService;

    const config = {
        get: () => undefined,
    } as unknown as ConfigService;

    const service = new RecommendationsService(
        repository,
        new FeatureBuilderService(),
        ml,
        mlConfig,
        config,
    );

    return { service, findCandidateQuestions, findReviewsForItems, recommend };
}

describe('RecommendationsService (slipping-soon v2)', () => {
    it('serves never-reviewed items on day one with nominal forget-date and options', async () => {
        const createdTenHoursAgo = new Date(Date.now() - 10 * HOUR);

        const item = makeItem({
            id: 'fresh',
            createdAt: createdTenHoursAgo,
            nextReviewAt: new Date(createdTenHoursAgo.getTime() + DAY),
            options: ['Paris', 'Lyon', 'Mars', 'Venus'],
        });

        const { service, recommend } = setup({
            findCandidateQuestions: jest.fn().mockResolvedValue([item]),
            findReviewsForItems: jest.fn().mockResolvedValue([]),
        });

        const response = await service.getRecommendations('user-1', 20);

        expect(recommend).not.toHaveBeenCalledTimes(0);

        expect(response.items).toHaveLength(1);

        expect(response.items[0].studyItemId).toBe('fresh');

        expect(response.items[0].options).toEqual([
            'Paris',
            'Lyon',
            'Mars',
            'Venus',
        ]);

        const expected = createdTenHoursAgo.getTime() + DAY;

        expect(
            Math.abs(
                new Date(response.items[0].expectedForgetDate!).getTime() -
                    expected,
            ),
        ).toBeLessThan(HOUR);
    });

    it('excludes items reviewed inside the refractory window', async () => {
        const twoHoursAgo = new Date(Date.now() - 2 * HOUR);

        const item = makeItem({
            id: 'resting',
            nextReviewAt: new Date(twoHoursAgo.getTime() + 2 * DAY),
        });

        const review = makeReview({
            studyItemId: 'resting',
            createdAt: twoHoursAgo,
        });

        const { service, recommend } = setup({
            findCandidateQuestions: jest.fn().mockResolvedValue([item]),
            findReviewsForItems: jest.fn().mockResolvedValue([review]),
        });

        const response = await service.getRecommendations('user-1', 20);

        expect(recommend).not.toHaveBeenCalled();

        expect(response.items).toEqual([]);
    });

    it('includes reviewed items whose forget-date falls inside the window', async () => {
        const twentyFiveHoursAgo = new Date(Date.now() - 25 * HOUR);

        const item = makeItem({
            id: 'due-ish',
            nextReviewAt: new Date(twentyFiveHoursAgo.getTime() + 2 * DAY),
        });

        const review = makeReview({
            studyItemId: 'due-ish',
            createdAt: twentyFiveHoursAgo,
        });

        const { service } = setup({
            findCandidateQuestions: jest.fn().mockResolvedValue([item]),
            findReviewsForItems: jest.fn().mockResolvedValue([review]),
        });

        const response = await service.getRecommendations('user-1', 20);

        expect(response.items).toHaveLength(1);

        expect(response.items[0].expectedForgetDate).toBeTruthy();
    });

    it('excludes healthy items whose forget-date lies beyond the window', async () => {
        const threeDaysAgo = new Date(Date.now() - 3 * DAY);

        const item = makeItem({
            id: 'healthy',
            nextReviewAt: new Date(threeDaysAgo.getTime() + 7 * DAY),
        });

        const review = makeReview({
            studyItemId: 'healthy',
            createdAt: threeDaysAgo,
        });

        const { service, recommend } = setup({
            findCandidateQuestions: jest.fn().mockResolvedValue([item]),
            findReviewsForItems: jest.fn().mockResolvedValue([review]),
        });

        const response = await service.getRecommendations('user-1', 20);

        expect(recommend).not.toHaveBeenCalled();

        expect(response.items).toEqual([]);
    });

    it('orders by earliest forget-date and respects the requested limit', async () => {
        const base = Date.now() - 10 * HOUR;

        const rows = [30, 20, 10].map((hoursAgo, index) =>
            makeItem({
                id: `item-${index}`,
                createdAt: new Date(base - hoursAgo * HOUR),
                nextReviewAt: new Date(base - hoursAgo * HOUR + DAY),
            }),
        );

        const { service } = setup({
            findCandidateQuestions: jest.fn().mockResolvedValue(rows),
            findReviewsForItems: jest.fn().mockResolvedValue([]),
        });

        const response = await service.getRecommendations('user-1', 2);

        expect(response.items.map((item) => item.studyItemId)).toEqual([
            'item-0',
            'item-1',
        ]);
    });

    it('maps ML ranking and preserves options and forget-dates', async () => {
        const tenHoursAgo = new Date(Date.now() - 10 * HOUR);

        const rows = [
            makeItem({
                id: 'alpha',
                createdAt: tenHoursAgo,
                nextReviewAt: new Date(tenHoursAgo.getTime() + DAY),
                options: ['a1', 'a2'],
            }),
            makeItem({
                id: 'beta',
                createdAt: new Date(Date.now() - 12 * HOUR),
                nextReviewAt: new Date(Date.now() - 12 * HOUR + DAY),
                options: ['b1', 'b2'],
            }),
        ];

        const { service, recommend } = setup(
            {
                findCandidateQuestions: jest.fn().mockResolvedValue(rows),
                findReviewsForItems: jest.fn().mockResolvedValue([]),
            },
            {
                recommend: jest.fn().mockResolvedValue({
                    model_name: 'gradient_boosting',
                    recommendations: [
                        {
                            rank: 1,
                            question_id: 'beta',
                            recall_probability: 0.31,
                            priority: 'high',
                        },
                        {
                            rank: 2,
                            question_id: 'alpha',
                            recall_probability: 0.62,
                            priority: 'medium',
                        },
                    ],
                }),
            },
        );

        const response = await service.getRecommendations('user-1', 20);

        expect(response.source).toBe('ml');

        expect(recommend).toHaveBeenCalledTimes(1);

        expect(response.items.map((item) => item.studyItemId)).toEqual([
            'beta',
            'alpha',
        ]);

        expect(response.items[0].options).toEqual(['b1', 'b2']);

        expect(response.items[0].expectedForgetDate).toBeTruthy();

        expect(response.items[0].recallProbability).toBeCloseTo(0.31, 6);
    });

    it('falls back to forgetting-date order when the ML call throws', async () => {
        const thirtyHoursAgo = new Date(Date.now() - 30 * HOUR);

        const rows = [
            makeItem({
                id: 'slower',
                createdAt: new Date(Date.now() - 14 * HOUR),
                nextReviewAt: new Date(Date.now() - 14 * HOUR + DAY),
            }),
            makeItem({
                id: 'slipperier',
                nextReviewAt: new Date(thirtyHoursAgo.getTime() + 1.2 * DAY),
            }),
        ];

        const review = makeReview({
            studyItemId: 'slipperier',
            createdAt: thirtyHoursAgo,
        });

        const { service } = setup(
            {
                findCandidateQuestions: jest.fn().mockResolvedValue(rows),
                findReviewsForItems: jest.fn().mockResolvedValue([review]),
            },
            {
                recommend: jest
                    .fn()
                    .mockRejectedValue(new Error('timeout of 2000ms exceeded')),
            },
        );

        const response = await service.getRecommendations('user-1', 5);

        expect(response.source).toBe('scheduler');

        expect(response.items.map((item) => item.studyItemId)).toEqual([
            'slipperier',
            'slower',
        ]);

        expect(response.items[0].recallProbability).toBeNull();

        expect(response.items[0].priority).toBe('high');
    });

    it('skips ML entirely when flagged unavailable', async () => {
        const item = makeItem({ id: 'only' });

        const { service, recommend } = setup(
            { findCandidateQuestions: jest.fn().mockResolvedValue([item]) },
            { isAvailable: false },
        );

        const response = await service.getRecommendations('user-1', 5);

        expect(recommend).not.toHaveBeenCalled();

        expect(response.source).toBe('scheduler');

        expect(response.items[0].studyItemId).toBe('only');
    });

    it('short-circuits with an empty list when no questions exist', async () => {
        const { service, recommend } = setup();

        const response = await service.getRecommendations('user-1', 10);

        expect(recommend).not.toHaveBeenCalled();

        expect(response.items).toEqual([]);
    });

    it('counts slipping-soon candidates for the dashboard', async () => {
        const rows = [
            makeItem({
                id: 'in-window',
                createdAt: new Date(Date.now() - 10 * HOUR),
                nextReviewAt: new Date(Date.now() - 10 * HOUR + DAY),
            }),
            makeItem({
                id: 'healthy',
                nextReviewAt: new Date(Date.now() + 4 * DAY),
            }),
        ];

        const review = makeReview({
            studyItemId: 'healthy',
            createdAt: new Date(Date.now() - 3 * DAY),
        });

        const { service } = setup({
            findCandidateQuestions: jest.fn().mockResolvedValue(rows),
            findReviewsForItems: jest.fn().mockResolvedValue([review]),
        });

        expect(await service.countSlippingSoon('user-1')).toBe(1);
    });
});
