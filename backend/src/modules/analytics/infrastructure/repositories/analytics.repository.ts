import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../prisma/prisma.service';

@Injectable()
export class AnalyticsRepository {
    constructor(private readonly prisma: PrismaService) {}

    async getTodayCounts(userId: string, now: Date) {
        const startOfToday = new Date(now);
        startOfToday.setUTCHours(0, 0, 0, 0);

        const [studyItems, dueToday, completedToday] = await Promise.all([
            this.prisma.studyItem.count({
                where: { userId },
            }),
            this.prisma.review.count({
                where: {
                    nextReviewAt: { lte: now },
                    studyItem: { userId },
                },
            }),
            this.prisma.review.count({
                where: {
                    reviewedAt: { gte: startOfToday },
                    studyItem: { userId },
                },
            }),
        ]);

        return { studyItems, dueToday, completedToday };
    }

    async getAccuracyCounts(userId: string, rangeStart: Date) {
        const [total, correct, incorrect] = await Promise.all([
            this.prisma.review.count({
                where: {
                    studyItem: { userId },
                    reviewedAt: { gte: rangeStart },
                    isCorrect: { not: null },
                },
            }),
            this.prisma.review.count({
                where: {
                    studyItem: { userId },
                    reviewedAt: { gte: rangeStart },
                    isCorrect: true,
                },
            }),
            this.prisma.review.count({
                where: {
                    studyItem: { userId },
                    reviewedAt: { gte: rangeStart },
                    isCorrect: false,
                },
            }),
        ]);

        return { total, correct, incorrect };
    }

    async getReviewDates(userId: string): Promise<string[]> {
        const rows = await this.prisma.$queryRaw<{ d: Date }[]>`
            SELECT DISTINCT ("reviewedAt" AT TIME ZONE 'UTC')::date AS d
            FROM "Review" r
            JOIN "StudyItem" s ON s."id" = r."studyItemId"
            WHERE s."userId" = ${userId}
            ORDER BY d ASC
        `;

        return rows.map((row) => {
            const date = row.d instanceof Date ? row.d : new Date(String(row.d));
            return date.toISOString().slice(0, 10);
        });
    }

    async getRangedReviews(userId: string, rangeStart: Date) {
        return this.prisma.review.findMany({
            where: {
                studyItem: { userId },
                reviewedAt: { gte: rangeStart },
            },
            select: {
                reviewedAt: true,
                isCorrect: true,
                responseTimeMs: true,
                sessionDurationMinutes: true,
                sessionId: true,
            },
            orderBy: { reviewedAt: 'asc' },
        });
    }

    async getSubjectRows(userId: string, rangeStart: Date, now: Date) {
        const [items, rangedReviews] = await Promise.all([
            this.prisma.studyItem.findMany({
                where: {
                    userId,
                    topicId: { not: null },
                },
                select: {
                    id: true,
                    nextReviewAt: true,
                    topic: {
                        select: {
                            chapter: {
                                select: {
                                    subject: {
                                        select: {
                                            id: true,
                                            name: true,
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            }),
            this.prisma.review.findMany({
                where: {
                    studyItem: {
                        userId,
                        topicId: { not: null },
                    },
                    reviewedAt: { gte: rangeStart },
                },
                select: {
                    isCorrect: true,
                    studyItem: {
                        select: {
                            id: true,
                            topic: {
                                select: {
                                    chapter: {
                                        select: {
                                            subject: {
                                                select: {
                                                    id: true,
                                                    name: true,
                                                },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            }),
        ]);

        interface SubjectCell {
            id: string;
            name: string;
            items: number;
            due: number;
            reviews: number;
            correct: number;
            incorrect: number;
        }

        const byId = new Map<string, SubjectCell>();

        const cellOf = (subject: { id: string; name: string } | null): SubjectCell | null => {
            if (!subject) {
                return null;
            }
            let cell = byId.get(subject.id);
            if (!cell) {
                cell = {
                    id: subject.id,
                    name: subject.name,
                    items: 0,
                    due: 0,
                    reviews: 0,
                    correct: 0,
                    incorrect: 0,
                };
                byId.set(subject.id, cell);
            }
            return cell;
        };

        for (const item of items) {
            const cell = cellOf(item.topic?.chapter?.subject ?? null);
            if (!cell) {
                continue;
            }
            cell.items += 1;
            if (item.nextReviewAt !== null && item.nextReviewAt <= now) {
                cell.due += 1;
            }
        }

        for (const review of rangedReviews) {
            const cell = cellOf(review.studyItem.topic?.chapter?.subject ?? null);
            if (!cell) {
                continue;
            }
            cell.reviews += 1;
            if (review.isCorrect === true) {
                cell.correct += 1;
            } else if (review.isCorrect === false) {
                cell.incorrect += 1;
            }
        }

        const rows = Array.from(byId.values()).map((cell) => {
            const graded = cell.correct + cell.incorrect;
            return {
                name: cell.name,
                items: cell.items,
                due: cell.due,
                reviews: cell.reviews,
                accuracyPct: graded > 0 ? Math.round((100 * cell.correct) / graded) : 0,
            };
        });

        rows.sort((a, b) => {
            if (b.reviews !== a.reviews) {
                return b.reviews - a.reviews;
            }
            if (b.due !== a.due) {
                return b.due - a.due;
            }
            return a.name.localeCompare(b.name);
        });

        return rows.slice(0, 8);
    }

    async getRecent(userId: string, limit: number) {
        return this.prisma.review.findMany({
            where: { studyItem: { userId } },
            orderBy: { reviewedAt: 'desc' },
            take: limit,
            select: {
                reviewedAt: true,
                isCorrect: true,
                studyItem: {
                    select: {
                        title: true,
                        content: true,
                        type: true,
                    },
                },
            },
        });
    }

    async getYearActivity(userId: string, since: Date) {
        return this.prisma.$queryRaw<{ d: Date; c: number }[]>`
            SELECT ("reviewedAt" AT TIME ZONE 'UTC')::date AS d,
                   COUNT(*)::int AS c
            FROM "Review" r
            JOIN "StudyItem" s ON s."id" = r."studyItemId"
            WHERE s."userId" = ${userId}
              AND "reviewedAt" >= ${since}
            GROUP BY d
            ORDER BY d ASC
        `;
    }
}
