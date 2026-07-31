import { Inject, Injectable } from '@nestjs/common';
import type { LlmProvider } from '../../domain/interfaces/llm-provider.interface';
import { GeneratedMetadataDto } from '../../presentation/dto/generated-metadata.dto';
import { GenerateMetadataRequest } from '../../domain/models/generate-metadata.request';
import { LLM_PROVIDER } from '../../infrastructure/tokens/llm-provider.token';
import { MetadataPrompt } from '../../infrastructure/prompts/metadata.prompt';

@Injectable()
export class MetadataService {
    constructor(
        @Inject(LLM_PROVIDER)
        private readonly llm: LlmProvider,
    ) {}

    async generateMetadata(
        request: GenerateMetadataRequest,
    ): Promise<GeneratedMetadataDto> {
        const prompt = new MetadataPrompt(request.text);

        return this.llm.generate(prompt);
    }
}
