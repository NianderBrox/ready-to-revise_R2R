import { DocumentStatus } from '@prisma/client';
import { DocumentAnalysisResult } from '../../../document-analysis/domain/models/document-analysis.result';

export class CreateDocumentResponseDto {
    constructor(
        public readonly documentId: string,
        public readonly status: DocumentStatus,
        public readonly analysis: DocumentAnalysisResult,
    ) {}
}
