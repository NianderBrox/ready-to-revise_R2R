import { Attachment } from '@prisma/client';

import { AttachmentResponseDto } from './dto/attachment-response.dto';

export class AttachmentsMapper {
  static toResponse(
    attachment: Attachment,
  ): AttachmentResponseDto {

    return {
      id: attachment.id,
      url: attachment.url,
      mimeType: attachment.mimeType ?? undefined,
      fileSize: attachment.fileSize ?? undefined,
      studyItemId: attachment.studyItemId,
      createdAt: attachment.createdAt,
      updatedAt: attachment.updatedAt,
      storageProvider: attachment.storageProvider ?? undefined,
    };
  }
}