import { Injectable } from '@nestjs/common';
import { FileContent } from '../files/value-objects/file-content';

@Injectable()
export class FileMapper {
    toFileContent(file: Express.Multer.File): FileContent {
        return new FileContent(file.buffer, file.mimetype, file.originalname);
    }
}
