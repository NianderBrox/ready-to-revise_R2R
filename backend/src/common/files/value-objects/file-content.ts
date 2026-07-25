export class FileContent {
    constructor(
        public readonly bytes: Buffer,

        public readonly mimeType: string,

        public readonly fileName: string,
    ) {}
}
