import { BadRequestException } from '@nestjs/common';

export class UnsupportedMimeTypeException extends BadRequestException {
    constructor(mimeType: string) {
        super(`Unsupported mime type: ${mimeType}`);
    }
}
