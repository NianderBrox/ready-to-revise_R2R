import { Buffer } from 'node:buffer';

export interface StorageProvider {
    save(documentId: string, file: Buffer, mimeType: string): Promise<string>;

    read(storageKey: string): Promise<Buffer>;

    delete(storageKey: string): Promise<void>;

    exists(storageKey: string): Promise<boolean>;
}
