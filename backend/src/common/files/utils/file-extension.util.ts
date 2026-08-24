// import { UnsupportedMimeTypeException } from 'src/modules/storage/exceptions/unsupported-mime-type.exception';

import { UnsupportedMimeTypeException } from '../../../modules/storage/domain/exceptions/unsupported-mime-type.exception';
import { DOCX_MIME_TYPE } from '../constants/document-mime.constants';

const MIME_TYPE_TO_EXTENSION: Record<string, string> = {
    'application/pdf': 'pdf',

    'image/png': 'png',

    'image/jpeg': 'jpg',

    'image/jpg': 'jpg',

    'image/webp': 'webp',

    [DOCX_MIME_TYPE]: 'docx',
};

export function getExtensionFromMimeType(mimeType: string): string {
    const extension = MIME_TYPE_TO_EXTENSION[mimeType];

    if (!extension) {
        throw new UnsupportedMimeTypeException(mimeType);
    }

    return extension;
}
