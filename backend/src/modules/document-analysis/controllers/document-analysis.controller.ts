import {
    Controller,
    Post,
    UploadedFile,
    UseInterceptors,
} from '@nestjs/common';

import { FileInterceptor } from '@nestjs/platform-express';

import { DocumentAnalysisService } from '../services/document-analysis.service';
import { AnalyzeDocumentRequest } from '../models/analyze-document.request';

import { FileContent } from '../../../common/files/value-objects/file-content';

@Controller('document-analysis')
export class DocumentAnalysisController {
    constructor(
        private readonly documentAnalysisService: DocumentAnalysisService,
    ) {}

    @Post()
    @UseInterceptors(
        FileInterceptor('file', {
            limits: {
                fileSize: 20 * 1024 * 1024,
            },
        }),
    )
    async analyze(
        @UploadedFile()
        file: Express.Multer.File,
    ) {
        const request = new AnalyzeDocumentRequest(
            new FileContent(file.buffer, file.mimetype, file.originalname),
        );

        return this.documentAnalysisService.analyze(request);
    }
}
