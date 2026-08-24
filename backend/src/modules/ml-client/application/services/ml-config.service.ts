import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class MlConfigService {
    private static readonly DEFAULT_TIMEOUT_MS = 2000;

    constructor(private readonly config: ConfigService) {}

    get baseUrl(): string {
        return (
            this.config.get<string>('ML_SERVICE_URL') ?? 'http://localhost:8000'
        );
    }

    get modelName(): string {
        return this.config.get<string>('ML_MODEL_NAME') ?? 'gradient_boosting';
    }

    get timeoutMs(): number {
        const raw = this.config.get<string>('ML_TIMEOUT_MS');

        const parsed = raw !== undefined ? Number(raw) : NaN;

        return Number.isFinite(parsed) && parsed > 0
            ? parsed
            : MlConfigService.DEFAULT_TIMEOUT_MS;
    }
}
