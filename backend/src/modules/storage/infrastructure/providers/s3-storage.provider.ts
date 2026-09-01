import { Injectable, Logger, OnModuleDestroy } from '@nestjs/common';
import {
    DeleteObjectCommand,
    GetObjectCommand,
    HeadObjectCommand,
    NotFound,
    PutObjectCommand,
    S3Client,
} from '@aws-sdk/client-s3';
import { S3ConfigService } from '../../application/services/s3-config.service';
import { StorageProvider } from '../../domain/interfaces/storage.provider';
import { getExtensionFromMimeType } from '../../../../common/files/utils/file-extension.util';

@Injectable()
export class S3StorageProvider implements StorageProvider, OnModuleDestroy {
    private readonly logger = new Logger(S3StorageProvider.name);

    private readonly client: S3Client;

    constructor(private readonly config: S3ConfigService) {
        this.client = new S3Client({
            region: this.config.region,

            endpoint: this.config.endpoint,

            forcePathStyle: this.config.forcePathStyle,

            credentials: {
                accessKeyId: this.config.accessKeyId,

                secretAccessKey: this.config.secretAccessKey,
            },
        });
    }

    private objectKey(documentId: string, extension: string): string {
        const prefix = this.config.prefix;

        const path = `${documentId}/original.${extension}`;

        return prefix ? `${prefix}/${path}` : path;
    }

    async save(
        documentId: string,
        file: Buffer,
        mimeType: string,
    ): Promise<string> {
        const extension = getExtensionFromMimeType(mimeType);

        const key = this.objectKey(documentId, extension);

        await this.client.send(
            new PutObjectCommand({
                Bucket: this.config.bucket,

                Key: key,

                Body: file,

                ContentType: mimeType,
            }),
        );

        return key;
    }

    async read(storageKey: string): Promise<Buffer> {
        const response = await this.client.send(
            new GetObjectCommand({
                Bucket: this.config.bucket,

                Key: storageKey,
            }),
        );

        const body = response.Body;

        if (body === undefined) {
            throw new Error(`Empty object body for key: ${storageKey}`);
        }

        return Buffer.from(await body.transformToByteArray());
    }

    async delete(storageKey: string): Promise<void> {
        try {
            await this.client.send(
                new DeleteObjectCommand({
                    Bucket: this.config.bucket,

                    Key: storageKey,
                }),
            );
        } catch (error) {
            if (error instanceof NotFound) {
                return;
            }

            throw error;
        }
    }

    async exists(storageKey: string): Promise<boolean> {
        try {
            await this.client.send(
                new HeadObjectCommand({
                    Bucket: this.config.bucket,

                    Key: storageKey,
                }),
            );

            return true;
        } catch (error) {
            if (error instanceof NotFound) {
                return false;
            }

            this.logger.warn(
                `Failed to check object existence for key ${storageKey}: ${String(error)}`,
            );

            return false;
        }
    }

    onModuleDestroy(): void {
        this.client.destroy();
    }
}
