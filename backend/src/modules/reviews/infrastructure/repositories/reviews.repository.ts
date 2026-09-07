import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../prisma/prisma.service';
import { CreateReviewData } from '../../domain/interfaces/create-review-data.interface';
import { DueQuestionRow } from '../../../recall-predictions/domain/interfaces/recall-data.interfaces';

export interface StudyItemGradingInfo {
    id: string;

    type: string;

    options: unknown;

    correctAnswerIndex: number | null;
}

@Injectable()
export class ReviewsRepository {
    constructor(private readonly prisma: PrismaService) {}

    async create(data: CreateReviewData) {
        return this.prisma.review.create({
            data,
        });
    }

    async findAllByStudyItem(studyItemId: string) {
        return this.prisma.review.findMany({
            where: {
                studyItemId,
            },
            orderBy: {
                reviewedAt: 'desc',
            },
        });
    }

    async findLatest(studyItemId: string) {
        return this.prisma.review.findFirst({
            where: {
                studyItemId,
            },
            orderBy: {
                reviewedAt: 'desc',
            },
        });
    }

    async getStudyItemGradingInfo(
        studyItemId: string,
        userId: string,
    ): Promise<StudyItemGradingInfo | null> {
        return this.prisma.studyItem.findFirst({
            where: {
                id: studyItemId,
                userId,
            },
            select: {
                id: true,
                type: true,
                options: true,
                correctAnswerIndex: true,
            },
        });
    }

    async getStudyItemData(
        studyItemId: string,
        userId: string,
    ): Promise<DueQuestionRow | null> {
        const item = await this.prisma.studyItem.findFirst({
            where: {
                id: studyItemId,
                userId,
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
        });

        if (!item) {
            return null;
        }

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

    async findAllAsRows(studyItemId: string) {
        return this.prisma.review.findMany({
            where: {
                studyItemId,
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

    async updateItemNextReviewAt(
        studyItemId: string,
        nextReviewAt: Date,
    ): Promise<void> {
        await this.prisma.studyItem.update({
            where: {
                id: studyItemId,
            },
            data: {
                nextReviewAt,
            },
        });
    }

    async studyItemExists(
        studyItemId: string,
        userId: string,
    ): Promise<boolean> {
        const studyItem = await this.prisma.studyItem.findUnique({
            where: {
                id: studyItemId,
                userId,
            },
            select: {
                id: true,
            },
        });

        return !!studyItem;
    }
}
