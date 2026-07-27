import { BadRequestException, Injectable, PipeTransform } from '@nestjs/common';

@Injectable()
export class FileValidationPipe implements PipeTransform<Express.Multer.File> {
    private static readonly ALLOWED_MIME_TYPES = [
        'application/pdf',
        'image/png',
        'image/jpeg',
    ];

    private static readonly MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

    transform(file: Express.Multer.File): Express.Multer.File {
        if (!file) {
            throw new BadRequestException('File is required.');
        }

        if (!FileValidationPipe.ALLOWED_MIME_TYPES.includes(file.mimetype)) {
            throw new BadRequestException(
                `Unsupported file type: ${file.mimetype}`,
            );
        }

        if (file.size > FileValidationPipe.MAX_FILE_SIZE) {
            throw new BadRequestException('File exceeds the 20 MB limit.');
        }

        return file;
    }
}
