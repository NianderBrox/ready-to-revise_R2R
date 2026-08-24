import { Injectable, Logger } from '@nestjs/common';
import mammoth from 'mammoth';
import { FileContent } from '../../../../common/files/value-objects/file-content';
import { DOCX_MIME_TYPE } from '../../../../common/files/constants/document-mime.constants';

const MAX_EXTRACTED_CHARS = 120_000;

@Injectable()
export class DocumentTextExtractorService {
    private readonly logger = new Logger(DocumentTextExtractorService.name);

    supports(file: FileContent): boolean {
        return file.mimeType === DOCX_MIME_TYPE;
    }

    async extract(file: FileContent): Promise<string> {
        try {
            const result = await mammoth.extractRawText({ buffer: file.bytes });

            const text = result.value.trim();

            if (!text) {
                throw new Error('Document contains no extractable text.');
            }

            this.logger.log(
                `Extracted ${text.length} chars from "${file.fileName}".`,
            );

            if (text.length > MAX_EXTRACTED_CHARS) {
                return text.slice(0, MAX_EXTRACTED_CHARS);
            }

            return text;
        } catch (error) {
            this.logger.warn(
                `docx text extraction failed for "${file.fileName}": ${String(error)}`,
            );

            throw new Error(
                'Could not read text from the Word document. ' +
                    'Make sure it is a valid .docx file with selectable text.',
            );
        }
    }
}
