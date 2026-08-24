import { Module } from '@nestjs/common';
import { AiModule } from '../ai/ai.module';
import { AnalyzeDocumentPrompt } from './infrastructure/prompts/analyze-document.prompt';
import { GeminiDocumentAnalysisProvider } from './infrastructure/providers/gemini-document-analysis.provider';
import { DOCUMENT_ANALYSIS_PROVIDER } from './infrastructure/tokens/document-analysis-provider.token';
import { DocumentAnalysisController } from './presentation/controllers/document-analysis.controller';
import { DocumentAnalysisService } from './application/services/document-analysis.service';
import { DocumentTextExtractorService } from './application/services/document-text-extractor.service';

@Module({
    imports: [AiModule],
    providers: [
        AnalyzeDocumentPrompt,
        DocumentAnalysisService,
        DocumentTextExtractorService,
        {
            provide: DOCUMENT_ANALYSIS_PROVIDER,
            useClass: GeminiDocumentAnalysisProvider,
        },
    ],
    exports: [DocumentAnalysisService, DocumentTextExtractorService],
    controllers: [DocumentAnalysisController],
})
export class DocumentAnalysisModule {}
