import { Module } from '@nestjs/common';

import { StorageService } from './services/storage.service';

import { LocalStorageProvider } from './providers/local-storage.provider';
import { ConfigModule } from '@nestjs/config';

import { StorageConfigService } from './services/storage-config.service';
import { STORAGE_PROVIDER } from './tokens/storage-provider.token';

@Module({
    imports: [ConfigModule],
    providers: [
        StorageConfigService,

        StorageService,

        {
            provide: STORAGE_PROVIDER,
            useClass: LocalStorageProvider,
        },
    ],

    exports: [StorageService, StorageConfigService],
})
export class StorageModule {}
