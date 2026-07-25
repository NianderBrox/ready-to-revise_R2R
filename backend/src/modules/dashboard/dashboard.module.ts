import { Module } from '@nestjs/common';

import { PrismaModule } from '../../prisma/prisma.module';

import { DashboardController } from './dashboard.controller';
import { DashboardRepository } from './dashboard.repository';
import { DashboardService } from './dashboard.service';

@Module({
  imports: [
    PrismaModule,
  ],
  controllers: [
    DashboardController,
  ],
  providers: [
    DashboardRepository,
    DashboardService,
  ],
})
export class DashboardModule {}