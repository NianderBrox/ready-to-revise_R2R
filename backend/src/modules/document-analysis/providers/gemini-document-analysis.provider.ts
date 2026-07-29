import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

import { GeminiClient } from '../../ai/clients/gemini.client';
import { AbstractGeminiProvider } from '../../ai/providers/abstract-gemini.provider';
import { JsonParser } from '../../ai/utils/json-parser';
import { InvalidAiResponseException } from '../../ai/exceptions/invalid-ai-response.exception';

import { AnalyzeDocumentPrompt } from '../prompts/analyze-document.prompt';
import { AnalyzeDocumentRequest } from '../models/analyze-document.request';
import { DocumentAnalysisResult } from '../models/document-analysis.result';
import { DocumentAnalysisProvider } from '../interfaces/document-analysis.provider';
import { AiConfigService } from '../../ai/services/ai-config.service';
// import { AiConfigService } from 'src/modules/ai/services/ai-config.service';

@Injectable()
export class GeminiDocumentAnalysisProvider
    extends AbstractGeminiProvider<
        AnalyzeDocumentRequest,
        {
            model: string;
            input: unknown;
        }
    >
    implements DocumentAnalysisProvider
{
    private readonly model: string;

    constructor(
        client: GeminiClient,
        private readonly aiConfig: AiConfigService,
        private readonly prompt: AnalyzeDocumentPrompt,
    ) {
        super(client);

        this.model = this.aiConfig.geminiModel;
    }

    protected buildInteractionRequest(request: AnalyzeDocumentRequest): {
        model: string;
        input: unknown;
    } {
        return {
            model: this.model,

            input: [
                {
                    type: 'text',
                    text: this.prompt.instruction(),
                },

                {
                    type: 'document',
                    data: request.file.bytes.toString('base64'),
                    mime_type: request.file.mimeType,
                },
            ],
        };
    }

    async analyze(
        request: AnalyzeDocumentRequest,
    ): Promise<DocumentAnalysisResult> {
        const output = await this.execute(request);

        try {
            return JsonParser.parse<DocumentAnalysisResult>(output);
        } catch {
            throw new InvalidAiResponseException();
        }
    }
}
