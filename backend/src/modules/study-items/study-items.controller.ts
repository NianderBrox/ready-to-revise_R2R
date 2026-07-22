import {
  Body,
  Controller,
  Post,
  UseGuards,
} from '@nestjs/common';

import { StudyItemsService } from './study-items.service';
import { CreateStudyItemDto } from './dto/create-study-item.dto';

import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';

import type { CurrentUserData } from '../../common/interfaces/current-user-data.interface';

@Controller('study-items')
@UseGuards(JwtAuthGuard)
export class StudyItemsController {
  constructor(
    private readonly studyItemsService: StudyItemsService,
  ) {}

  @Post()
  async create(
    @CurrentUser() user: CurrentUserData,
    @Body() dto: CreateStudyItemDto,
  ) {
    const studyItem = await this.studyItemsService.create(
      user.userId,
      dto,
    );

    return studyItem;
  }
}