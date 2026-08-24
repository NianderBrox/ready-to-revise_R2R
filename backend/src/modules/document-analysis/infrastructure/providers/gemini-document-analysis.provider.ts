import { Injectable } from '@nestjs/common';
import { AbstractGeminiProvider } from '../../../ai/infrastructure/providers/abstract-gemini.provider';
import { AnalyzeDocumentPrompt } from '../prompts/analyze-document.prompt';
import { DocumentAnalysisProvider } from '../../domain/interfaces/document-analysis.provider';
import { AiConfigService } from '../../../ai/application/services/ai-config.service';
import { AnalyzeDocumentRequest } from '../../domain/models/analyze-document.request';
import { GeminiClient } from '../../../ai/infrastructure/clients/gemini.client';
import { DocumentAnalysisResult } from '../../domain/models/document-analysis.result';

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
    constructor(
        client: GeminiClient,
        private readonly aiConfig: AiConfigService,
        private readonly prompt: AnalyzeDocumentPrompt,
    ) {
        super(client);
    }

    protected buildInteractionRequest(request: AnalyzeDocumentRequest): {
        model: string;
        input: unknown;
    } {
        const contentParts: Array<Record<string, unknown>> = [
            {
                type: 'text',
                text: this.prompt.instruction(),
            },
        ];

        if (request.extractedText) {
            contentParts.push({
                type: 'text',

                text:
                    'The following is the extracted text of a study document. ' +
                    'Analyze it according to the instructions above.\n\n' +
                    request.extractedText,
            });
        } else {
            contentParts.push({
                type: 'document',
                data: request.file.bytes.toString('base64'),
                mime_type: request.file.mimeType,
            });
        }

        return {
            model: this.aiConfig.geminiModel,

            input: contentParts,
        };
    }

    async analyze(
        request: AnalyzeDocumentRequest,
    ): Promise<DocumentAnalysisResult> {
        return this.executeAndParse<DocumentAnalysisResult>(request);
    }
}
