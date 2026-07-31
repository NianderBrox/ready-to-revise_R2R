// import { UnsupportedMimeTypeException } from 'src/modules/storage/exceptions/unsupported-mime-type.exception';

import { UnsupportedMimeTypeException } from '../../../modules/storage/domain/exceptions/unsupported-mime-type.exception';

const MIME_TYPE_TO_EXTENSION: Record<string, string> = {
    'application/pdf': 'pdf',

    'image/png': 'png',

    'image/jpeg': 'jpg',

    'image/jpg': 'jpg',

    'image/webp': 'webp',
};

export function getExtensionFromMimeType(mimeType: string): string {
    const extension = MIME_TYPE_TO_EXTENSION[mimeType];

    if (!extension) {
        throw new UnsupportedMimeTypeException(mimeType);
    }

    return extension;
}
