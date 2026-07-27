import { DocumentAnalysisResult } from '../../document-analysis/models/document-analysis.result';
import { DocumentStatus } from '@prisma/client';

export class CreateDocumentResponseDto {
    constructor(
        public readonly documentId: string,
        public readonly status: DocumentStatus,
        public readonly analysis: DocumentAnalysisResult,
    ) {}
}
