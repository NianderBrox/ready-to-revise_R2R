export class GenerateMetadataRequest {
    constructor(
        public readonly text: string,
        public readonly language?: string,
        public readonly subjectHint?: string,
    ) {}
}
