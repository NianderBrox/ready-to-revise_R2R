import { join } from 'path';

export class StoragePath {
    static documentDirectory(uploadDirectory: string, documentId: string) {
        return join(uploadDirectory, documentId);
    }

    static originalDocument(
        uploadDirectory: string,
        documentId: string,
        extension: string,
    ) {
        return join(uploadDirectory, documentId, `original.${extension}`);
    }
}
