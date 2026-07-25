export interface CreateAttachmentData {
  url: string;

  storageProvider?: string;

  mimeType?: string;

  fileSize?: number;

  studyItemId: string;
}