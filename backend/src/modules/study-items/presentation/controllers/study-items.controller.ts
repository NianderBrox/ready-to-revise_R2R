import {
    Body,
    Controller,
    Delete,
    Get,
    Param,
    ParseUUIDPipe,
    Patch,
    Post,
    UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { CreateStudyItemCommand } from '../../application/commands/create-study-item.command';
import { StudyItemsService } from '../../application/services/study-items.service';
import { CreateStudyItemDto } from '../dto/create-study-item.dto';
import { UpdateStudyItemDto } from '../dto/update-study-item.dto';

@Controller('study-items')
@UseGuards(JwtAuthGuard)
export class StudyItemsController {
    constructor(private readonly studyItemsService: StudyItemsService) {}

    @Post()
    async create(
        @CurrentUser() user: CurrentUserData,
        @Body() dto: CreateStudyItemDto,
    ) {
        const command: CreateStudyItemCommand = {
            userId: user.userId,
            title: dto.title,
            content: dto.content,
            type: dto.type,
            difficulty: dto.difficulty,
            topicId: dto.topicId,
        };

        return this.studyItemsService.create(command);
    }

    @Get()
    async findAll(@CurrentUser() user: CurrentUserData) {
        return this.studyItemsService.findAll(user.userId);
    }

    @Get(':id')
    async findOne(
        @Param('id', ParseUUIDPipe) id: string,
        @CurrentUser() user: CurrentUserData,
    ) {
        return this.studyItemsService.findOne(id, user.userId);
    }

    @Patch(':id')
    async update(
        @Param('id', ParseUUIDPipe) id: string,
        @CurrentUser() user: CurrentUserData,
        @Body() dto: UpdateStudyItemDto,
    ) {
        return this.studyItemsService.update(id, user.userId, dto);
    }

    @Delete(':id')
    async remove(
        @Param('id', ParseUUIDPipe) id: string,
        @CurrentUser() user: CurrentUserData,
    ) {
        await this.studyItemsService.remove(id, user.userId);

        return {
            message: 'Study item deleted successfully.',
        };
    }
}
