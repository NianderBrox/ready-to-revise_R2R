import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class AiConfigService {
    constructor(private readonly config: ConfigService) {}

    get geminiModel(): string {
        return this.config.getOrThrow<string>('GEMINI_MODEL');
    }

    get geminiApiKey(): string {
        return this.config.getOrThrow<string>('GEMINI_API_KEY');
    }
}
