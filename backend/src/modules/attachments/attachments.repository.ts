import { Injectable } from '@nestjs/common';

import { PrismaService } from '../../prisma/prisma.service';

import { CreateAttachmentData } from './interfaces/create-attachment-data.interface';

@Injectable()
export class AttachmentsRepository {
  constructor(
    private readonly prisma: PrismaService,
  ) {}

  async create(
    data: CreateAttachmentData,
  ) {
    return this.prisma.attachment.create({
      data,
    });
  }

  async findByStudyItem(
    studyItemId: string,
  ) {
    return this.prisma.attachment.findMany({
      where: {
        studyItemId,
      },
      orderBy: {
        createdAt: 'desc',
      },
    });
  }

  async delete(id: string) {
    await this.prisma.attachment.delete({
      where: {
        id,
      },
    });
  }

  async studyItemBelongsToUser(
    studyItemId: string,
    userId: string,
  ): Promise<boolean> {

    const studyItem =
      await this.prisma.studyItem.findFirst({
        where: {
          id: studyItemId,
          userId,
        },
        select: {
          id: true,
        },
      });

    return !!studyItem;
  }

  async findById(id: string) {
    return this.prisma.attachment.findUnique({
      where: {
        id,
      },
    });
  }
}