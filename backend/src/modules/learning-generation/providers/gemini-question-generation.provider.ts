import { Injectable } from '@nestjs/common';

import { GeminiClient } from '../../ai/clients/gemini.client';
import { AbstractGeminiProvider } from '../../ai/providers/abstract-gemini.provider';
import { InvalidAiResponseException } from '../../ai/exceptions/invalid-ai-response.exception';
import { JsonParser } from '../../ai/utils/json-parser';
import { AiConfigService } from '../../ai/services/ai-config.service';

import { GenerateQuestionsPrompt } from '../prompts/generate-questions.prompt';
import { GenerateQuestionsRequest } from '../models/generate-questions.request';
import { QuestionGenerationResult } from '../models/question-generation.result';
import { QuestionGenerationProvider } from '../interfaces/question-generation.provider';

@Injectable()
export class GeminiQuestionGenerationProvider
    extends AbstractGeminiProvider<
        GenerateQuestionsRequest,
        {
            model: string;
            input: unknown;
        }
    >
    implements QuestionGenerationProvider
{
    private readonly model: string;

    constructor(
        client: GeminiClient,
        private readonly aiConfig: AiConfigService,
        private readonly prompt: GenerateQuestionsPrompt,
    ) {
        super(client);

        this.model = this.aiConfig.geminiModel;
    }

    protected buildInteractionRequest(request: GenerateQuestionsRequest): {
        model: string;
        input: unknown;
    } {
        return {
            model: this.model,

            input: [
                {
                    type: 'text',
                    text: this.prompt.build(request.title, request.context),
                },
                {
                    type: 'document',
                    data: request.file.bytes.toString('base64'),
                    mime_type: request.file.mimeType,
                },
            ],
        };
    }

    async generate(
        request: GenerateQuestionsRequest,
    ): Promise<QuestionGenerationResult> {
        const output = await this.execute(request);

        try {
            return JsonParser.parse<QuestionGenerationResult>(output);
        } catch {
            throw new InvalidAiResponseException();
        }
    }
}
