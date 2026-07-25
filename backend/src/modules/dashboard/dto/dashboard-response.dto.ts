import { DashboardActivityDto } from './dashboard-activity.dto';
import { DashboardAiDto } from './dashboard-ai.dto';
import { DashboardProgressDto } from './dashboard-progress.dto';
import { DashboardReviewsDto } from './dashboard-reviews.dto';
import { DashboardStatsDto } from './dashboard-stats.dto';
import { DashboardUserDto } from './dashboard-user.dto';

export class DashboardResponseDto {
    user!: DashboardUserDto;

    stats!: DashboardStatsDto;

    reviews!: DashboardReviewsDto;

    progress!: DashboardProgressDto;

    recentActivity!: DashboardActivityDto[];

    ai!: DashboardAiDto;
}
