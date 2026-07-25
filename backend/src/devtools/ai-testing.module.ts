import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { AiModule } from '../modules/ai/ai.module';
import { DocumentAnalysisModule } from '../modules/document-analysis/document-analysis.module';

@Module({
    imports: [
        ConfigModule.forRoot({
            isGlobal: true,
        }),

        AiModule,

        DocumentAnalysisModule,
    ],
})
export class AiTestingModule {}
