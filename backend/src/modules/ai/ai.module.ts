import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { GeminiClient } from './clients/gemini.client';
import { GeminiProvider } from './providers/gemini.provider';

import { LLM_PROVIDER } from './tokens/llm-provider.token';
import { MetadataService } from './services/metadata.service';
import { AiController } from './controllers/ai.controller';
import { AiConfigService } from './services/ai-config.service';

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
