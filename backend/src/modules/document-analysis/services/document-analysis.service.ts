import { Inject, Injectable } from '@nestjs/common';

import { AnalyzeDocumentRequest } from '../models/analyze-document.request';
import { DocumentAnalysisResult } from '../models/document-analysis.result';
import type { DocumentAnalysisProvider } from '../interfaces/document-analysis.provider';
import { DOCUMENT_ANALYSIS_PROVIDER } from '../tokens/document-analysis-provider.token';

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
