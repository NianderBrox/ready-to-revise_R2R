import {
    Controller,
    Post,
    UploadedFile,
    UseGuards,
    UseInterceptors,
} from '@nestjs/common';

import { FileInterceptor } from '@nestjs/platform-express';

import { CurrentUser } from '../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../common/interfaces/current-user-data.interface';

import { JwtAuthGuard } from '../../auth/guards/jwt-auth.guard';

import { CreateDocumentCommand } from '../commands/create-document.command';
import { DocumentsService } from '../services/documents.service';
import { FileMapper } from 'src/common/mappers/file.mapper';
import { FileValidationPipe } from 'src/common/pipes/file-validation.pipe';
import { DocumentUploadService } from '../application/services/document-upload.service';

@Controller('documents')
@UseGuards(JwtAuthGuard)
export class DocumentsController {
    constructor(
        private readonly documentUploadService: DocumentUploadService,

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
}
