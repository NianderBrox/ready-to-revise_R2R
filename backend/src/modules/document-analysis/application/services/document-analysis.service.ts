import { Inject, Injectable } from '@nestjs/common';
import type { DocumentAnalysisProvider } from '../../domain/interfaces/document-analysis.provider';
import { AnalyzeDocumentRequest } from '../../domain/models/analyze-document.request';
import { DocumentAnalysisResult } from '../../domain/models/document-analysis.result';
import { DOCUMENT_ANALYSIS_PROVIDER } from '../../infrastructure/tokens/document-analysis-provider.token';

@Injectable()
export class DocumentAnalysisService {
    constructor(
        @Inject(DOCUMENT_ANALYSIS_PROVIDER)
        private readonly provider: DocumentAnalysisProvider,
    ) {}

    async analyze(
        request: AnalyzeDocumentRequest,
    ): Promise<DocumentAnalysisResult> {
        return this.provider.analyze(request);
    }
}
