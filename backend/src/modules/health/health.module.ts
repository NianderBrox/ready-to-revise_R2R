import { Module } from '@nestjs/common';
import { HealthService } from './application/services/health.service';
import { HealthController } from './presentation/controllers/health.controller';

@Module({
    controllers: [HealthController],
    providers: [HealthService],
})
export class HealthModule {}
