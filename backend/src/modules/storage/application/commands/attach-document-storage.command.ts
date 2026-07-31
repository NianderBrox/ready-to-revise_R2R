interface AttachDocumentStorageCommandData {
    storageKey: string;

    fileSize: number;

    checksum?: string;
}

export class AttachDocumentStorageCommand {
    readonly storageKey: string;

    readonly fileSize: number;

    readonly checksum?: string;

    constructor(data: AttachDocumentStorageCommandData) {
        this.storageKey = data.storageKey;
        this.fileSize = data.fileSize;
        this.checksum = data.checksum;
    }
}
