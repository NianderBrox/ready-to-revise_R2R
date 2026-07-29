import { Injectable } from '@nestjs/common';

import { promises as fs } from 'fs';

import { join } from 'path';

import { StorageProvider } from '../interfaces/storage.provider';
import { StorageConfigService } from '../services/storage-config.service';
import { getExtensionFromMimeType } from '../../../common/files/utils/file-extension.util';
import { StoragePath } from '../utils/storage-path.util';

@Injectable()
export class LocalStorageProvider implements StorageProvider {
    constructor(private readonly config: StorageConfigService) {}

    private getDocumentDirectory(documentId: string): string {
        return join(this.config.uploadDirectory, documentId);
    }

    // private getOriginalFilePath(documentId: string, extension: string): string {
    //     return join(
    //         this.getDocumentDirectory(documentId),
    //         `original.${extension}`,
    //     );
    // }

    async save(
        documentId: string,
        file: Buffer,
        mimeType: string,
    ): Promise<string> {
        const directory = this.getDocumentDirectory(documentId);

        await fs.mkdir(directory, {
            recursive: true,
        });

        const extension = getExtensionFromMimeType(mimeType);

        const filePath = StoragePath.originalDocument(
            this.config.uploadDirectory,
            documentId,
            extension,
        );

        await fs.writeFile(filePath, file);

        return filePath;
    }

    async read(storageKey: string): Promise<Buffer> {
        return fs.readFile(storageKey);
    }

    async delete(storageKey: string): Promise<void> {
        await fs.rm(storageKey, {
            force: true,
        });
    }

    async exists(storageKey: string): Promise<boolean> {
        try {
            await fs.access(storageKey);

            return true;
        } catch {
            return false;
        }
    }
}
