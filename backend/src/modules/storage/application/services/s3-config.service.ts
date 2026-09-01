import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class S3ConfigService {
    constructor(private readonly config: ConfigService) {}

    get bucket(): string {
        return this.config.getOrThrow<string>('S3_BUCKET');
    }

    get endpoint(): string {
        return this.config.getOrThrow<string>('S3_ENDPOINT');
    }

    get region(): string {
        return this.config.get<string>('S3_REGION', 'auto');
    }

    get accessKeyId(): string {
        return this.config.getOrThrow<string>('S3_ACCESS_KEY_ID');
    }

    get secretAccessKey(): string {
        return this.config.getOrThrow<string>('S3_SECRET_ACCESS_KEY');
    }

    get forcePathStyle(): boolean {
        const raw = this.config.get<string>('S3_FORCE_PATH_STYLE', 'true');

        return raw.toLowerCase() === 'true';
    }

    get prefix(): string {
        const raw = this.config.get<string>('S3_PREFIX', '');

        return raw.replace(/^\/+|\/+$/g, '');
    }
}
