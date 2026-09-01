import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class MlConfigService {
    private static readonly DEFAULT_TIMEOUT_MS = 2000;

    private static readonly DEFAULT_FAILURE_COOLDOWN_MS = 30000;

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

    get failureCooldownMs(): number {
        const raw = this.config.get<string>('ML_FAILURE_COOLDOWN_MS');

        const parsed = raw !== undefined ? Number(raw) : NaN;

        return Number.isFinite(parsed) && parsed > 0
            ? parsed
            : MlConfigService.DEFAULT_FAILURE_COOLDOWN_MS;
    }
}
