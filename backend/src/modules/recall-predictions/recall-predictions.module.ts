import { Module } from '@nestjs/common';

import { PrismaModule } from '../../prisma/prisma.module';

import { RecallQueryRepository } from './infrastructure/repositories/recall-query.repository';
import { FeatureBuilderService } from './application/services/feature-builder.service';
import { RecommendationsService } from './application/services/recommendations.service';
import { RecommendationsController } from './presentation/controllers/recommendations.controller';

@Module({
    imports: [PrismaModule],
    controllers: [RecommendationsController],
    providers: [
        RecallQueryRepository,
        FeatureBuilderService,
        RecommendationsService,
    ],
    exports: [
        RecommendationsService,
        RecallQueryRepository,
        FeatureBuilderService,
    ],
})
export class RecallPredictionsModule {}
