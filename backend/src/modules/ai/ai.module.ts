import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { GeminiProvider } from './infrastructure/providers/gemini.provider';
import { LLM_PROVIDER } from './infrastructure/tokens/llm-provider.token';
import { MetadataService } from './application/services/metadata.service';
import { AiConfigService } from './application/services/ai-config.service';
import { AiController } from './presentation/controllers/ai.controller';
import { GeminiClient } from './infrastructure/clients/gemini.client';

@Module({
    controllers: [AiController],
    imports: [ConfigModule],
    providers: [
        GeminiClient,
        MetadataService,
        {
            provide: LLM_PROVIDER,
            useClass: GeminiProvider,
        },
        AiConfigService,
    ],
    exports: [LLM_PROVIDER, MetadataService, AiConfigService, GeminiClient],
})
export class AiModule {}
