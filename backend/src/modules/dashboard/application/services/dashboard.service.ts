import { Injectable } from '@nestjs/common';
import { DashboardResponseDto } from '../../presentation/dto/dashboard-response.dto';
import { DashboardRepository } from '../../infrastructure/repositories/dashboard.repository';
import { MlHttpService } from '../../../ml-client/infrastructure/http/ml-http.service';
import { RecommendationsService } from '../../../recall-predictions/application/services/recommendations.service';

@Injectable()
export class DashboardService {
    constructor(
        private readonly repository: DashboardRepository,
        private readonly mlHttp: MlHttpService,
        private readonly recommendationsService: RecommendationsService,
    ) {}

    async getDashboard(userId: string): Promise<DashboardResponseDto> {
        const stats = await this.repository.getDashboardStats(userId);

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
                slippingSoon: await this.slippingSoonCount(userId),
            },

            progress: {
                completionPercentage: 0,
                streakDays: 0,
            },

            recentActivity: [],

            ai: {
                suggestion: await this.atRiskSuggestion(userId),
            },
        };
    }

    private async slippingSoonCount(userId: string): Promise<number> {
        try {
            return await this.recommendationsService.countSlippingSoon(userId);
        } catch {
            return 0;
        }
    }

    private async atRiskSuggestion(userId: string): Promise<string | null> {
        if (!this.mlHttp.isAvailable) {
            return null;
        }

        try {
            const top = await this.recommendationsService.getAtRiskTop(userId);

            if (!top) {
                return null;
            }

            const label = top.title ?? 'a question';

            const probability =
                top.recallProbability !== null
                    ? ` (recall ${(top.recallProbability * 100).toFixed(0)}%)`
                    : '';

            return `Revise "${label}" next${probability}.`;
        } catch {
            return null;
        }
    }
}
