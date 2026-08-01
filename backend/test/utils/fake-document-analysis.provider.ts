import { Difficulty } from '@prisma/client';
import { DocumentAnalysisProvider } from '../../src/modules/document-analysis/domain/interfaces/document-analysis.provider';
import { AnalyzeDocumentRequest } from '../../src/modules/document-analysis/domain/models/analyze-document.request';
import { DocumentAnalysisResult } from '../../src/modules/document-analysis/domain/models/document-analysis.result';

export class FakeDocumentAnalysisProvider implements DocumentAnalysisProvider {
    async analyze(
        request: AnalyzeDocumentRequest,
    ): Promise<DocumentAnalysisResult> {
        return new DocumentAnalysisResult(
            'This is extracted test document content.',
            'Test Document',
            'A test document for E2E testing.',
            'Biology',
            'Cell Biology',
            'Cell Structure',
            Difficulty.MEDIUM,
            ['cell', 'biology', 'structure'],
        );
    }
}
