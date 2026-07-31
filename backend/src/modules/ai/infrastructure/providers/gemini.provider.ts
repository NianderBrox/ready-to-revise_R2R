import { Injectable } from '@nestjs/common';
import { LlmProvider } from '../../domain/interfaces/llm-provider.interface';
import { AbstractGeminiProvider } from './abstract-gemini.provider';
import { AiConfigService } from '../../application/services/ai-config.service';
import { GeminiClient } from '../clients/gemini.client';
import { Prompt } from '../prompts/prompt.interface';

@Injectable()
export class GeminiProvider
    extends AbstractGeminiProvider<
        string,
        {
            model: string;
            input: string;
        }
    >
    implements LlmProvider
{
    private readonly model: string;

    constructor(
        client: GeminiClient,
        private readonly aiConfig: AiConfigService,
    ) {
        super(client);

        this.model = this.aiConfig.geminiModel;
    }

    protected buildInteractionRequest(prompt: string): {
        model: string;
        input: string;
    } {
        return {
            model: this.model,
            input: prompt,
        };
    }

    async generate<T>(prompt: Prompt<T>): Promise<T> {
        const output = await this.execute(prompt.build());

        return prompt.parse(output);
    }
}
