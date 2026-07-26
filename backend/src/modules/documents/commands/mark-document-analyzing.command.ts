interface MarkDocumentAnalyzingCommandData {
    startedAt?: Date;
}

export class MarkDocumentAnalyzingCommand {
    readonly startedAt?: Date;

    constructor(data: MarkDocumentAnalyzingCommandData = {}) {
        this.startedAt = data.startedAt;
    }
}
