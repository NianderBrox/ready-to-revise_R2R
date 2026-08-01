import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../prisma/prisma.service';
import { CreateReviewData } from '../../domain/interfaces/create-review-data.interface';

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
