import { Injectable } from '@nestjs/common';
import { DashboardResponseDto } from '../../presentation/dto/dashboard-response.dto';
import { DashboardRepository } from '../../infrastructure/repositories/dashboard.repository';
import { RecommendationsService } from '../../../recall-predictions/application/services/recommendations.service';

@Injectable()
export class DashboardService {
    constructor(
        private readonly repository: DashboardRepository,
        private readonly recommendationsService: RecommendationsService,
    ) {}

    async getDashboard(
        userId: string,
        dueBefore?: Date,
    ): Promise<DashboardResponseDto> {
        const stats = await this.repository.getDashboardStats(
            userId,
            dueBefore,
        );

        return {
            user: {
                name: stats.user?.name ?? '',
            },

            stats: {
                studyItems: stats.studyItems,
                inboxItems: stats.inboxItems,

                subjects: 0,
                chapters: 0,
                topics: 0,
            },

            reviews: {
                dueToday: stats.dueToday,
                upcoming: stats.upcomingReviews,
                completedToday: stats.completedToday,
                slippingSoon: await this.slippingSoonCount(userId, dueBefore),
            },

            progress: {
                completionPercentage: 0,
                streakDays: 0,
            },

            recentActivity: [],

            ai: {
                suggestion: await this.atRiskSuggestion(userId, dueBefore),
            },
        };
    }

    private async slippingSoonCount(
        userId: string,
        dueBefore?: Date,
    ): Promise<number> {
        try {
            return await this.recommendationsService.countSlippingSoon(
                userId,
                dueBefore,
            );
        } catch {
            return 0;
        }
    }

    private async atRiskSuggestion(
        userId: string,
        dueBefore?: Date,
    ): Promise<string | null> {
        try {
            const top = await this.recommendationsService.getAtRiskTop(
                userId,
                dueBefore,
            );

            if (!top) {
                return null;
            }

            const label = top.title ?? 'a question';

            return `Revise "${label}" next.`;
        } catch {
            return null;
        }
    }
}
