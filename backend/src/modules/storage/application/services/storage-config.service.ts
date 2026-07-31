import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class StorageConfigService {
    constructor(private readonly config: ConfigService) {}

    get uploadDirectory(): string {
        return this.config.get<string>('UPLOAD_DIRECTORY', 'uploads');
    }
}
