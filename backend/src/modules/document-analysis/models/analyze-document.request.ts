import { FileContent } from '../../../common/files/value-objects/file-content';

export class AnalyzeDocumentRequest {
    constructor(public readonly file: FileContent) {}
}
