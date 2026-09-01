import { Injectable } from '@nestjs/common';

import { PrismaService } from '../../../../prisma/prisma.service';

import { DueQuestionRow } from '../../domain/interfaces/recall-data.interfaces';

const MAX_CANDIDATES = 500;

@Injectable()
export class RecallQueryRepository {
    constructor(private readonly prisma: PrismaService) {}

    async findDueQuestions(
        userId: string,
        subjectId?: string,
    ): Promise<DueQuestionRow[]> {
        const items = await this.prisma.studyItem.findMany({
            where: {
                userId,

                type: 'QUESTION',

                nextReviewAt: { lte: new Date() },

                ...(subjectId !== undefined
                    ? {
                          topic: {
                              chapter: { subjectId },
                          },
                      }
                    : {}),
            },

            include: {
                topic: {
                    include: {
                        chapter: {
                            include: {
                                subject: true,
                            },
                        },
                    },
                },
            },

            orderBy: {
                nextReviewAt: 'asc',
            },

            take: MAX_CANDIDATES,
        });

        return items.map((item) => this.toRow(item));
    }

    async findCandidateQuestions(
        userId: string,
        subjectId?: string,
    ): Promise<DueQuestionRow[]> {
        const items = await this.prisma.studyItem.findMany({
            where: {
                userId,

                type: 'QUESTION',

                ...(subjectId !== undefined
                    ? {
                          topic: {
                              chapter: { subjectId },
                          },
                      }
                    : {}),
            },

            include: {
                topic: {
                    include: {
                        chapter: {
                            include: {
                                subject: true,
                            },
                        },
                    },
                },
            },

            orderBy: [{ nextReviewAt: 'asc' }, { createdAt: 'asc' }],

            take: MAX_CANDIDATES,
        });

        return items.map((item) => this.toRow(item));
    }

    private toRow(item: {
        id: string;
        title: string | null;
        content: string | null;
        difficulty: string | null;
        nextReviewAt: Date | null;
        createdAt: Date;
        mediaDocumentId: string | null;
        options: unknown;
        topic?: {
            name: string | null;
            chapter?: { subject?: { name: string | null } | null } | null;
        } | null;
    }): DueQuestionRow {
        return {
            id: item.id,

            title: item.title,

            content: item.content,

            difficulty: item.difficulty,

            nextReviewAt: item.nextReviewAt,

            createdAt: item.createdAt,

            mediaDocumentId: item.mediaDocumentId,

            options: Array.isArray(item.options)
                ? item.options.filter(
                      (option): option is string => typeof option === 'string',
                  )
                : null,

            topicName: item.topic?.name ?? null,

            subjectName: item.topic?.chapter?.subject?.name ?? null,
        };
    }

    async findReviewsForItems(studyItemIds: string[]) {
        if (studyItemIds.length === 0) {
            return [];
        }

        return this.prisma.review.findMany({
            where: {
                studyItemId: { in: studyItemIds },
            },

            select: {
                studyItemId: true,

                isCorrect: true,

                confidenceScore: true,

                responseTimeMs: true,

                hesitationMs: true,

                answerChanges: true,

                createdAt: true,
            },

            orderBy: {
                createdAt: 'desc',
            },
        });
    }

    async findUserReviewRows(userId: string) {
        return this.prisma.review.findMany({
            where: {
                studyItem: { userId },
            },

            select: {
                isCorrect: true,

                confidenceScore: true,

                responseTimeMs: true,

                hesitationMs: true,
            },
        });
    }

    async countUserDueQuestions(userId: string): Promise<number> {
        return this.prisma.studyItem.count({
            where: {
                userId,

                type: 'QUESTION',

                nextReviewAt: { lte: new Date() },
            },
        });
    }
}
