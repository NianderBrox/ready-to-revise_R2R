interface MarkDocumentReadyCommandData {
    title: string;
}

export class MarkDocumentReadyCommand {
    readonly title: string;

    constructor(data: MarkDocumentReadyCommandData) {
        this.title = data.title;
    }
}
