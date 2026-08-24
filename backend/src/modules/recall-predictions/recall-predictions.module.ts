import { Module } from '@nestjs/common';

import { PrismaModule } from '../../prisma/prisma.module';
import { MlClientModule } from '../ml-client/ml-client.module';

import { RecallQueryRepository } from './infrastructure/repositories/recall-query.repository';
import { FeatureBuilderService } from './application/services/feature-builder.service';
import { RecommendationsService } from './application/services/recommendations.service';
import { RecommendationsController } from './presentation/controllers/recommendations.controller';

@Module({
    imports: [PrismaModule, MlClientModule],
    controllers: [RecommendationsController],
    providers: [
        RecallQueryRepository,
        FeatureBuilderService,
        RecommendationsService,
    ],
    exports: [RecommendationsService],
})
export class RecallPredictionsModule {}
