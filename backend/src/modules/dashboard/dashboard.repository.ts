import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class DashboardRepository {
  constructor(
    private readonly prisma: PrismaService,
  ) {}

  async getDashboardStats(userId: string) {
    const now = new Date();

    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);

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