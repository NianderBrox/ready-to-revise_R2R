import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

import { GeminiClient } from '../clients/gemini.client';
import { LlmProvider } from '../interfaces/llm-provider.interface';
import { Prompt } from '../prompts/prompt.interface';
import { JsonParser } from '../utils/json-parser';
import { AbstractGeminiProvider } from './abstract-gemini.provider';
import { AiConfigService } from '../services/ai-config.service';

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
