interface SaveFileCommandData {
    documentId: string;

    file: Buffer;

    mimeType: string;
}

export class SaveFileCommand {
    readonly documentId: string;

    readonly file: Buffer;

    readonly mimeType: string;

    constructor(data: SaveFileCommandData) {
        this.documentId = data.documentId;
        this.file = data.file;
        this.mimeType = data.mimeType;
    }
}
