interface CreateDocumentCommandData {
    userId: string;
    originalName: string;
    mimeType: string;
}

export class CreateDocumentCommand {
    readonly userId: string;

    readonly originalName: string;

    readonly mimeType: string;

    constructor(data: CreateDocumentCommandData) {
        this.userId = data.userId;
        this.originalName = data.originalName;
        this.mimeType = data.mimeType;
    }
}
