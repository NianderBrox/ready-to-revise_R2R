import { Inject, Injectable } from '@nestjs/common';

import type { LlmProvider } from '../interfaces/llm-provider.interface';
import { GeneratedMetadataDto } from '../dto/generated-metadata.dto';
import { MetadataPrompt } from '../prompts/metadata.prompt';
import { GenerateMetadataRequest } from '../models/generate-metadata.request';
import { LLM_PROVIDER } from '../tokens/llm-provider.token';

@Injectable()
export class MetadataService {
  constructor(
    @Inject(LLM_PROVIDER)
    private readonly llm: LlmProvider,
  ) {}

  async generateMetadata(
    request: GenerateMetadataRequest,
  ): Promise<GeneratedMetadataDto> {
    const prompt = new MetadataPrompt(
      request.text,
    );

    return this.llm.generateObject<GeneratedMetadataDto>(
      prompt,
    );
  }
}