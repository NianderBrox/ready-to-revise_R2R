import { DocumentAnalysisResult } from '../../document-analysis/models/document-analysis.result';

interface MarkDocumentReadyCommandData {
    analysis: DocumentAnalysisResult;
}

export class MarkDocumentReadyCommand {
    readonly analysis: DocumentAnalysisResult;

    constructor(data: MarkDocumentReadyCommandData) {
        this.analysis = data.analysis;
    }
}
