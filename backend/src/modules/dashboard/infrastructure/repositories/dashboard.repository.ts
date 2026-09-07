import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../prisma/prisma.service';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

@Injectable()
export class DashboardRepository {
    constructor(private readonly prisma: PrismaService) {}

    async getDashboardStats(userId: string, dueBefore?: Date) {
        const now = dueBefore ?? new Date();

        const startOfToday = dueBefore
            ? new Date(dueBefore.getTime() - MS_PER_DAY)
            : new Date(now);

        if (!dueBefore) {
            startOfToday.setHours(0, 0, 0, 0);
        }

        const [
            user,
            studyItems,
            inboxItems,
            dueToday,
            upcomingReviews,
            completedToday,
        ] = await Promise.all([
            this.prisma.user.findUnique({
                where: {
                    id: userId,
                },
                select: {
                    name: true,
                },
            }),

            this.prisma.studyItem.count({
                where: {
                    userId,
                },
            }),

            this.prisma.studyItem.count({
                where: {
                    userId,
                    topicId: null,
                },
            }),

            this.prisma.review.count({
                where: {
                    nextReviewAt: {
                        lte: now,
                    },
                    studyItem: {
                        userId,
                    },
                },
            }),

            this.prisma.review.count({
                where: {
                    nextReviewAt: {
                        gt: now,
                    },
                    studyItem: {
                        userId,
                    },
                },
            }),

            this.prisma.review.count({
                where: {
                    reviewedAt: {
                        gte: startOfToday,
                    },
                    studyItem: {
                        userId,
                    },
                },
            }),
        ]);

        return {
            user,
            studyItems,
            inboxItems,
            dueToday,
            upcomingReviews,
            completedToday,
        };
    }
}
