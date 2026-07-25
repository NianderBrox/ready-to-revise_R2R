import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateSubjectData } from '../study-items/interfaces/create-subject-data.interface';

@Injectable()
export class SubjectsRepository {
  constructor(
    private readonly prisma: PrismaService,
  ) {}

  async create(data: CreateSubjectData) {
    return this.prisma.subject.create({
      data,
    });
  }

  async findAll() {
    return this.prisma.subject.findMany({
      orderBy: {
        name: 'asc',
      },
    });
  }

  async findById(id: string) {
    return this.prisma.subject.findUnique({
      where: {
        id,
      },
    });
  }

  async findByNormalizedName(
    name: string,
  ) {
    return this.prisma.subject.findFirst({
      where: {
        name: {
          equals: name,
          mode: 'insensitive',
        },
      },
    });
  }

  async update(
    id: string,
    name: string,
  ) {
    return this.prisma.subject.update({
      where: {
        id,
      },
      data: {
        name,
      },
    });
  }

  async delete(id: string) {
    await this.prisma.subject.delete({
      where: {
        id,
      },
    });
  }
}