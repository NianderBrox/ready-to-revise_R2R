import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../prisma/prisma.service';
import { CreateReviewData } from '../../domain/interfaces/create-review-data.interface';

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
