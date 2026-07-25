import { Module } from '@nestjs/common';

import { AiModule } from '../ai/ai.module';

import { AnalyzeDocumentPrompt } from './prompts/analyze-document.prompt';
import { GeminiDocumentAnalysisProvider } from './providers/gemini-document-analysis.provider';
import { DocumentAnalysisService } from './services/document-analysis.service';
import { DOCUMENT_ANALYSIS_PROVIDER } from './tokens/document-analysis-provider.token';
import { DocumentAnalysisController } from './controllers/document-analysis.controller';

@Module({
    imports: [AiModule],
    providers: [
        AnalyzeDocumentPrompt,
        DocumentAnalysisService,
        {
            provide: DOCUMENT_ANALYSIS_PROVIDER,
            useClass: GeminiDocumentAnalysisProvider,
        },
    ],
    exports: [DocumentAnalysisService],
    controllers: [DocumentAnalysisController],
})
export class DocumentAnalysisModule {}
