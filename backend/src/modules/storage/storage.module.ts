import { Module } from '@nestjs/common';
import { LocalStorageProvider } from './infrastructure/providers/local-storage.provider';
import { S3StorageProvider } from './infrastructure/providers/s3-storage.provider';
import { ConfigModule } from '@nestjs/config';
import { STORAGE_PROVIDER } from './infrastructure/tokens/storage-provider.token';
import { StorageConfigService } from './application/services/storage-config.service';
import { S3ConfigService } from './application/services/s3-config.service';
import { StorageService } from './application/services/storage.service';

@Module({
    imports: [ConfigModule],
    providers: [
        StorageConfigService,

        S3ConfigService,

        StorageService,

        {
            provide: STORAGE_PROVIDER,
            inject: [StorageConfigService, S3ConfigService],
            useFactory: (
                storageConfig: StorageConfigService,
                s3Config: S3ConfigService,
            ) => {
                if (storageConfig.driver === 's3') {
                    return new S3StorageProvider(s3Config);
                }

                return new LocalStorageProvider(storageConfig);
            },
        },
    ],

    exports: [StorageService, StorageConfigService],
})
export class StorageModule {}
