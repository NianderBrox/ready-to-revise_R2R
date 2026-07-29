import { Inject, Injectable } from '@nestjs/common';

import { STORAGE_PROVIDER } from '../tokens/storage-provider.token';

import type { StorageProvider } from '../interfaces/storage.provider';
import { SaveFileCommand } from '../commands/save-file.command';

@Injectable()
export class StorageService {
    constructor(
        @Inject(STORAGE_PROVIDER)
        private readonly storage: StorageProvider,
    ) {}

    save(command: SaveFileCommand): Promise<string> {
        return this.storage.save(
            command.documentId,
            command.file,
            command.mimeType,
        );
    }

    read(storageKey: string) {
        return this.storage.read(storageKey);
    }

    delete(storageKey: string) {
        return this.storage.delete(storageKey);
    }

    exists(storageKey: string) {
        return this.storage.exists(storageKey);
    }
}
