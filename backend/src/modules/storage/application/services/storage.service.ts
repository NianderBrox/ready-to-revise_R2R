import { Inject, Injectable } from '@nestjs/common';
import { SaveFileCommand } from '../commands/save-file.command';
import type { StorageProvider } from '../../domain/interfaces/storage.provider';
import { STORAGE_PROVIDER } from '../../infrastructure/tokens/storage-provider.token';

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
