import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class GeminiClient {
  private client: any;

  constructor(
    private readonly config: ConfigService,
  ) {}

  
  async getClient() {
    if (!this.client) {
      const { GoogleGenAI } =
        await import('@google/genai');

      this.client = new GoogleGenAI({
        apiKey: this.config.getOrThrow<string>(
          'GEMINI_API_KEY',
        ),
      });
    }

    return this.client;
  }
}