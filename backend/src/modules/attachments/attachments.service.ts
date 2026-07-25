import {
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { AttachmentsRepository } from './attachments.repository';
import { AttachmentsMapper } from './attachments.mapper';

import { CreateAttachmentDto } from './dto/create-attachment.dto';
import { AttachmentResponseDto } from './dto/attachment-response.dto';

@Injectable()
export class AttachmentsService {
  constructor(
    private readonly repository: AttachmentsRepository,
  ) {}

  async create(
    userId: string,
    dto: CreateAttachmentDto,
  ): Promise<AttachmentResponseDto> {

    const exists =
      await this.repository.studyItemBelongsToUser(
        dto.studyItemId,
        userId,
      );

    if (!exists) {
      throw new NotFoundException(
        'Study item not found or access denied.',
      );
    }

    const attachment =
      await this.repository.create({
        url: dto.url,
        storageProvider: dto.storageProvider,
        mimeType: dto.mimeType,
        fileSize: dto.fileSize,
        studyItemId: dto.studyItemId,
      });

    return AttachmentsMapper.toResponse(
      attachment,
    );
  }

  async findByStudyItem(
    userId: string,
    studyItemId: string,
  ): Promise<AttachmentResponseDto[]> {

    const exists =
      await this.repository.studyItemBelongsToUser(
        studyItemId,
        userId,
      );

    if (!exists) {
      throw new NotFoundException(
        'Study item not found or access denied.',
      );
    }

    const attachments =
      await this.repository.findByStudyItem(
        studyItemId,
      );

    return attachments.map(
      AttachmentsMapper.toResponse,
    );
  }

  async remove(
    userId: string,
    id: string,
  ): Promise<void> {

    const attachment =
      await this.repository.findById(id);

    if (!attachment) {
      throw new NotFoundException(
        'Attachment not found.',
      );
    }

    const allowed =
      await this.repository.studyItemBelongsToUser(
        attachment.studyItemId,
        userId,
      );

    if (!allowed) {
      throw new NotFoundException(
        'Attachment not found or access denied.',
      );
    }

    await this.repository.delete(id);
  }
}