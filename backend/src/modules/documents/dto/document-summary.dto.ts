export class DocumentSummaryResponseDto {
    constructor(
        public readonly id: string,

        public readonly title: string | null,

        public readonly originalName: string,

        public readonly mimeType: string,

        public readonly status: string,

        public readonly createdAt: Date,
    ) {}
}
