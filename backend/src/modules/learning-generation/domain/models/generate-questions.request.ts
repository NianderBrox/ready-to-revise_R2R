import { FileContent } from '../../../../common/files/value-objects/file-content';

export class GenerateQuestionsRequest {
    constructor(
        public readonly file: FileContent,
        public readonly title?: string,
        public readonly context?: string,
    ) {}
}
