import {
    Controller,
    Get,
    Param,
    Post,
    Res,
    UploadedFile,
    UseGuards,
    UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import type { Response } from 'express';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { FileMapper } from '../../../../common/mappers/file.mapper';
import { FileValidationPipe } from '../../../../common/pipes/file-validation.pipe';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { CreateDocumentCommand } from '../../application/commands/create-document.command';
import { DocumentUploadService } from '../../application/services/document-upload.service';
import { DocumentsService } from '../../application/services/documents.service';

@Controller('documents')
@UseGuards(JwtAuthGuard)
export class DocumentsController {
    constructor(
        private readonly documentUploadService: DocumentUploadService,

        private readonly documentsService: DocumentsService,

        private readonly fileMapper: FileMapper,
    ) {}

    @Post()
    @UseInterceptors(FileInterceptor('file'))
    async upload(
        @CurrentUser()
        user: CurrentUserData,

        @UploadedFile(FileValidationPipe)
        file: Express.Multer.File,
    ) {
        const command = new CreateDocumentCommand({
            userId: user.userId,

            originalName: file.originalname,

            mimeType: file.mimetype,
        });

        return this.documentUploadService.upload(
            this.fileMapper.toFileContent(file),

            command,
        );
    }

    @Get()
    async findAll(
        @CurrentUser()
        user: CurrentUserData,
    ) {
        return this.documentsService.listForUser(user.userId);
    }

    @Get(':id')
    async findOne(
        @Param('id')
        id: string,

        @CurrentUser()
        user: CurrentUserData,
    ) {
        return this.documentsService.findByIdForUser(
            id,

            user.userId,
        );
    }

    @Get(':id/file')
    async download(
        @Param('id')
        id: string,

        @CurrentUser()
        user: CurrentUserData,

        @Res()
        response: Response,
    ) {
        const result = await this.documentsService.getFileForUser(
            id,
            user.userId,
        );

        response.setHeader('Content-Type', result.document.mimeType);

        response.setHeader(
            'Content-Disposition',
            `inline; filename="${result.document.originalName}"`,
        );

        response.send(result.file.bytes);
    }
}
