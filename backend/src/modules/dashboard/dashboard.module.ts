import { Module } from '@nestjs/common';
import { PrismaModule } from '../../prisma/prisma.module';
import { DashboardController } from './presentation/controllers/dashboard.controller';
import { DashboardService } from './application/services/dashboard.service';
import { DashboardRepository } from './infrastructure/repositories/dashboard.repository';

@Module({
    imports: [PrismaModule],
    controllers: [DashboardController],
    providers: [DashboardRepository, DashboardService],
})
export class DashboardModule {}
