import {
    Body,
    Controller,
    Delete,
    Get,
    Param,
    ParseUUIDPipe,
    Patch,
    Post,
    Query,
    UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { StudyItemsService } from '../../application/services/study-items.service';
import { CreateStudyItemCommandMapper } from '../../application/mappers/create-study-item-command.mapper';
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
        const command = CreateStudyItemCommandMapper.fromDto(user.userId, dto);

        return this.studyItemsService.create(command);
    }

    @Get()
    async findAll(
        @CurrentUser() user: CurrentUserData,
        @Query('type') type?: string,
        @Query('due') due?: string,
    ) {
        return this.studyItemsService.findAll(user.userId, {
            type,

            due: due === 'true',
        });
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
