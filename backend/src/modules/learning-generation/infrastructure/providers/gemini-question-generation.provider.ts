import { Injectable } from '@nestjs/common';
import { AbstractGeminiProvider } from '../../../ai/infrastructure/providers/abstract-gemini.provider';
import { AiConfigService } from '../../../ai/application/services/ai-config.service';
import { GenerateQuestionsPrompt } from '../prompts/generate-questions.prompt';
import { QuestionGenerationProvider } from '../../domain/interfaces/question-generation.provider';
import { GeminiClient } from '../../../ai/infrastructure/clients/gemini.client';
import { GenerateQuestionsRequest } from '../../domain/models/generate-questions.request';
import { QuestionGenerationResult } from '../../domain/models/question-generation.result';

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
    constructor(
        client: GeminiClient,
        private readonly aiConfig: AiConfigService,
        private readonly prompt: GenerateQuestionsPrompt,
    ) {
        super(client);
    }

    protected buildInteractionRequest(request: GenerateQuestionsRequest): {
        model: string;
        input: unknown;
    } {
        return {
            model: this.aiConfig.geminiModel,

            input: [
                {
                    type: 'text',
                    text: this.prompt.build(request.metadata),
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
        return this.executeAndParse<QuestionGenerationResult>(request);
    }
}
