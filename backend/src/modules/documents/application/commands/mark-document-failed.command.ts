//Marked for delete!!

interface MarkDocumentFailedCommandData {
    reason?: string;
}

export class MarkDocumentFailedCommand {
    readonly reason?: string;

    constructor(data: MarkDocumentFailedCommandData = {}) {
        this.reason = data.reason;
    }
}
