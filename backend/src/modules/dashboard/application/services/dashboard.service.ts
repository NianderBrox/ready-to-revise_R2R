import { Injectable } from '@nestjs/common';
import { DashboardResponseDto } from '../../presentation/dto/dashboard-response.dto';
import { DashboardRepository } from '../../infrastructure/repositories/dashboard.repository';

@Injectable()
export class DashboardService {
    constructor(private readonly repository: DashboardRepository) {}

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
            },

            progress: {
                completionPercentage: 0,
                streakDays: 0,
            },

            recentActivity: [],

            ai: {
                suggestion: null,
            },
        };
    }
}
