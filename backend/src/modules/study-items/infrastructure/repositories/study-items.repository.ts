import { Injectable } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../../../../prisma/prisma.service';
import { CreateStudyItemData } from '../../domain/interfaces/create-study-item-data.interface';
import { StudyItemType } from '../../../../common/enums';

@Injectable()
export class StudyItemsRepository {
    constructor(private readonly prisma: PrismaService) {}

    async create(data: CreateStudyItemData) {
        return this.prisma.studyItem.create({
            data: {
                title: data.title,
                content: data.content,
                type: data.type,
                difficulty: data.difficulty,
                topicId: data.topicId,
                userId: data.userId,
                options: data.options ?? undefined,
                correctAnswerIndex: data.correctAnswerIndex ?? undefined,
                origin: data.origin ?? undefined,
                mediaDocumentId: data.mediaDocumentId ?? undefined,
                nextReviewAt: data.nextReviewAt ?? undefined,
            },
        });
    }

    async findAllByUser(userId: string, filters: FindFilters = {}) {
        return this.prisma.studyItem.findMany({
            where: {
                userId,
                type: filters.type,
                ...(filters.due ? { nextReviewAt: { lte: new Date() } } : {}),
            },
            orderBy: {
                createdAt: 'desc',
            },
        });
    }

    async findById(id: string, userId: string) {
        return this.prisma.studyItem.findFirst({
            where: {
                id,
                userId,
            },
        });
    }

    async topicExists(topicId: string): Promise<boolean> {
        const exists = await this.prisma.topic.findUnique({
            where: {
                id: topicId,
            },
            select: {
                id: true,
            },
        });

        return !!exists;
    }

    async update(id: string, data: Prisma.StudyItemUpdateInput) {
        return this.prisma.studyItem.update({
            where: { id },
            data,
        });
    }

    async delete(id: string) {
        await this.prisma.studyItem.delete({
            where: {
                id,
            },
        });
    }

    async createMany(data: CreateStudyItemData[]) {
        return this.prisma.$transaction(
            data.map((item) =>
                this.prisma.studyItem.create({
                    data: {
                        title: item.title,
                        content: item.content,
                        type: item.type,
                        difficulty: item.difficulty,
                        topicId: item.topicId,
                        userId: item.userId,
                        options: item.options ?? undefined,
                        correctAnswerIndex:
                            item.correctAnswerIndex ?? undefined,
                        origin: item.origin ?? undefined,
                        mediaDocumentId: item.mediaDocumentId ?? undefined,
                        nextReviewAt: item.nextReviewAt ?? undefined,
                    },
                }),
            ),
        );
    }
}

export interface FindFilters {
    type?: StudyItemType;
    due?: boolean;
}
