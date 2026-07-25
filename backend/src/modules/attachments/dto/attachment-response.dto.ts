export class AttachmentResponseDto {
  id!: string;

  url!: string;

  mimeType?: string;

  fileSize?: number;

  studyItemId!: string;

  createdAt!: Date;

  updatedAt!: Date;

  storageProvider?: string;
}