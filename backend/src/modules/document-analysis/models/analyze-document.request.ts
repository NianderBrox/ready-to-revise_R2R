export class AnalyzeDocumentRequest {
  constructor(
    public readonly file: Buffer,
    public readonly mimeType: string,
    public readonly fileName?: string,
  ) {}
}