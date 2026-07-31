import {
    Body,
    Controller,
    Delete,
    Get,
    Param,
    ParseUUIDPipe,
    Post,
    UseGuards,
} from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { AttachmentsService } from '../../application/services/attachments.service';
import { CreateAttachmentDto } from '../dto/create-attachment.dto';

@Controller('attachments')
@UseGuards(JwtAuthGuard)
export class AttachmentsController {
    constructor(private readonly attachmentsService: AttachmentsService) {}

    @Post()
    async create(
        @CurrentUser()
        user: CurrentUserData,

        @Body()
        dto: CreateAttachmentDto,
    ) {
        return this.attachmentsService.create(user.userId, dto);
    }

    @Get(':studyItemId')
    async findByStudyItem(
        @CurrentUser()
        user: CurrentUserData,

        @Param('studyItemId', ParseUUIDPipe)
        studyItemId: string,
    ) {
        return this.attachmentsService.findByStudyItem(
            user.userId,
            studyItemId,
        );
    }

    @Delete(':id')
    async remove(
        @CurrentUser()
        user: CurrentUserData,

        @Param('id', ParseUUIDPipe)
        id: string,
    ) {
        await this.attachmentsService.remove(user.userId, id);

        return {
            success: true,
            message: 'Attachment deleted successfully.',
        };
    }
}
