import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
} from '@nestjs/common';

import { ChaptersService } from './chapters.service';

import { CreateChapterDto } from './dto/create-chapter.dto';
import { UpdateChapterDto } from './dto/update-chapter.dto';

@Controller('chapters')
export class ChaptersController {
  constructor(
    private readonly chaptersService: ChaptersService,
  ) {}

  @Post()
  async create(
    @Body() dto: CreateChapterDto,
  ) {
    return this.chaptersService.create(dto);
  }

  @Get()
  async findAll() {
    return this.chaptersService.findAll();
  }

  @Get(':id')
  async findOne(
    @Param('id', ParseUUIDPipe)
    id: string,
  ) {
    return this.chaptersService.findOne(id);
  }

  @Patch(':id')
  async update(
    @Param('id', ParseUUIDPipe)
    id: string,

    @Body()
    dto: UpdateChapterDto,
  ) {
    return this.chaptersService.update(
      id,
      dto,
    );
  }

  @Delete(':id')
  async remove(
    @Param('id', ParseUUIDPipe)
    id: string,
  ) {
    await this.chaptersService.remove(id);

    return {
      success: true,
      message:
        'Chapter deleted successfully.',
    };
  }
}