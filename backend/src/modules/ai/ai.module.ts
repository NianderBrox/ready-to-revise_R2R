import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { GeminiClient } from './clients/gemini.client';
import { GeminiProvider } from './providers/gemini.provider';

import { LLM_PROVIDER } from './tokens/llm-provider.token';
import { MetadataService } from './services/metadata.service';
import { AiController } from './controllers/ai.controller';

@Module({
  controllers: [
      AiController,
  ],
  imports: [
    ConfigModule,
  ],
  providers: [
    GeminiClient,
    MetadataService,
    {
      provide: LLM_PROVIDER,
      useClass: GeminiProvider,
    },
  ],
  exports: [
    LLM_PROVIDER,
    MetadataService,
  ],
})
export class AiModule {}