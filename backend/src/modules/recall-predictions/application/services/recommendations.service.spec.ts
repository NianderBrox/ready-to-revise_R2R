import { RecommendationsService } from './recommendations.service';
import { RecallQueryRepository } from '../../infrastructure/repositories/recall-query.repository';
import { ConfigService } from '@nestjs/config';
import { DueQuestionRow } from '../../domain/interfaces/recall-data.interfaces';

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
        mediaDocumentId: null,
        options: ['A', 'B', 'C', 'D'],
        topicName: 'T',
        subjectName: 'S',
        ...overrides,
    };
}

function setup(
    repoOverride: {
        findDueQuestions?: jest.Mock;
        countUserDueQuestions?: jest.Mock;
    } = {},
) {
    const findDueQuestions =
        repoOverride.findDueQuestions ?? jest.fn().mockResolvedValue([]);

    const countUserDueQuestions =
        repoOverride.countUserDueQuestions ?? jest.fn().mockResolvedValue(0);

    const repository = {
        findDueQuestions,
        countUserDueQuestions,
    } as unknown as RecallQueryRepository;

    const config = {
        get: () => undefined,
    } as unknown as ConfigService;

    const service = new RecommendationsService(repository, config);

    return { service, findDueQuestions, countUserDueQuestions };
}

describe('RecommendationsService (due-today)', () => {
    it('returns only due questions with options and nextReviewAt', async () => {
        const dueAt = new Date(Date.now() - 2 * DAY);

        const item = makeItem({
            id: 'fresh',
            nextReviewAt: dueAt,
            options: ['Paris', 'Lyon', 'Mars', 'Venus'],
        });

        const { service, findDueQuestions } = setup({
            findDueQuestions: jest.fn().mockResolvedValue([item]),
        });

        const response = await service.getRecommendations('user-1', 20);

        expect(findDueQuestions).toHaveBeenCalledWith(
            'user-1',
            undefined,
            undefined,
        );

        expect(response.source).toBe('scheduler');

        expect(response.items).toHaveLength(1);

        expect(response.items[0].studyItemId).toBe('fresh');

        expect(response.items[0].options).toEqual([
            'Paris',
            'Lyon',
            'Mars',
            'Venus',
        ]);

        expect(response.items[0].nextReviewAt).toBe(dueAt.toISOString());

        expect(response.items[0].rank).toBe(1);
    });

    it('passes the subject filter and dueBefore boundary through', async () => {
        const { service, findDueQuestions } = setup();

        const dueBefore = new Date(Date.now() + HOUR);

        await service.getRecommendations('user-1', 10, 'subject-1', dueBefore);

        expect(findDueQuestions).toHaveBeenCalledWith(
            'user-1',
            'subject-1',
            dueBefore,
        );
    });

    it('orders by ascending nextReviewAt (most overdue first)', async () => {
        const rows = [
            makeItem({
                id: 'overdue',
                nextReviewAt: new Date(Date.now() - 3 * DAY),
            }),
            makeItem({
                id: 'due-now',
                nextReviewAt: new Date(Date.now() - HOUR),
            }),
        ];

        const { service } = setup({
            findDueQuestions: jest.fn().mockResolvedValue(rows),
        });

        const response = await service.getRecommendations('user-1', 10);

        expect(response.items.map((item) => item.studyItemId)).toEqual([
            'overdue',
            'due-now',
        ]);

        expect(response.items.map((item) => item.rank)).toEqual([1, 2]);
    });

    it('respects the requested limit', async () => {
        const rows = [10, 20, 30].map((hoursAgo, index) =>
            makeItem({
                id: `item-${index}`,
                nextReviewAt: new Date(Date.now() - hoursAgo * HOUR),
            }),
        );

        const { service } = setup({
            findDueQuestions: jest.fn().mockResolvedValue(rows),
        });

        const response = await service.getRecommendations('user-1', 2);

        expect(response.items.map((item) => item.studyItemId)).toEqual([
            'item-0',
            'item-1',
        ]);
    });

    it('clamps the limit to the daily cap', async () => {
        const rows = Array.from({ length: 30 }, (_, index) =>
            makeItem({
                id: `item-${index}`,
                nextReviewAt: new Date(Date.now() - 100 * HOUR),
            }),
        );

        const { service } = setup({
            findDueQuestions: jest.fn().mockResolvedValue(rows),
        });

        const response = await service.getRecommendations('user-1', 1000);

        expect(response.items).toHaveLength(20);
    });

    it('returns an empty list when nothing is due', async () => {
        const { service } = setup();

        const response = await service.getRecommendations('user-1', 10);

        expect(response.items).toEqual([]);
    });

    it('exposes the most overdue due question as the at-risk top', async () => {
        const rows = [
            makeItem({
                id: 'overdue',
                nextReviewAt: new Date(Date.now() - 3 * DAY),
            }),
            makeItem({
                id: 'due-now',
                nextReviewAt: new Date(Date.now() - HOUR),
            }),
        ];

        const { service } = setup({
            findDueQuestions: jest.fn().mockResolvedValue(rows),
        });

        const top = await service.getAtRiskTop('user-1');

        expect(top?.studyItemId).toBe('overdue');
    });

    it('returns null at-risk top when nothing is due', async () => {
        const { service } = setup();

        expect(await service.getAtRiskTop('user-1')).toBeNull();
    });

    it('counts due questions for the dashboard', async () => {
        const { service, countUserDueQuestions } = setup({
            countUserDueQuestions: jest.fn().mockResolvedValue(3),
        });

        expect(await service.countSlippingSoon('user-1')).toBe(3);

        expect(countUserDueQuestions).toHaveBeenCalledWith('user-1', undefined);
    });

    it('passes dueBefore through to the due count', async () => {
        const { service, countUserDueQuestions } = setup();

        const dueBefore = new Date(Date.now() + DAY);

        await service.countSlippingSoon('user-1', dueBefore);

        expect(countUserDueQuestions).toHaveBeenCalledWith('user-1', dueBefore);
    });
});
