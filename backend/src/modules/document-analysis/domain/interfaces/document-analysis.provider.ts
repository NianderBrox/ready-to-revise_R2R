import { AnalyzeDocumentRequest } from '../models/analyze-document.request';
import { DocumentAnalysisResult } from '../models/document-analysis.result';

export interface DocumentAnalysisProvider {
    analyze(request: AnalyzeDocumentRequest): Promise<DocumentAnalysisResult>;
}
