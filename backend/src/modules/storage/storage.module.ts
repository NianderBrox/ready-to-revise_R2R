import { Module } from '@nestjs/common';
import { LocalStorageProvider } from './infrastructure/providers/local-storage.provider';
import { ConfigModule } from '@nestjs/config';
import { STORAGE_PROVIDER } from './infrastructure/tokens/storage-provider.token';
import { StorageConfigService } from './application/services/storage-config.service';
import { StorageService } from './application/services/storage.service';

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
