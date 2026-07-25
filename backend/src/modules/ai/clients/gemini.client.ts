import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AiConfigService } from '../services/ai-config.service';

@Injectable()
export class GeminiClient {
    private client: any;

    constructor(private readonly aiConfig: AiConfigService) {}

    async getClient() {
        if (!this.client) {
            const { GoogleGenAI } = await import('@google/genai');

            this.client = new GoogleGenAI({
                apiKey: this.aiConfig.geminiApiKey,
            });
        }

        return this.client;
    }
}
