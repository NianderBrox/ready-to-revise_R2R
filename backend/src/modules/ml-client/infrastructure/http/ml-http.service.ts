import { Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { AxiosError, AxiosResponse } from 'axios';
import { firstValueFrom, timeout, catchError, throwError } from 'rxjs';

import { MlConfigService } from '../../application/services/ml-config.service';
import {
    MlHealthResponse,
    MlPredictRequest,
    MlPredictionResponse,
    MlRecommendRequest,
    MlRecommendResponse,
} from '../../domain/models/ml-contract.models';

const HEALTH_ENDPOINT = '/health';

const PREDICT_ENDPOINT = '/predict';

const RECOMMEND_ENDPOINT = '/recommend-revisions';

@Injectable()
export class MlHttpService {
    private readonly logger = new Logger(MlHttpService.name);

    private available = true;

    private lastFailureAt: number | null = null;

    constructor(
        private readonly http: HttpService,
        private readonly config: MlConfigService,
    ) {}

    get isAvailable(): boolean {
        if (this.available) {
            return true;
        }

        if (
            this.lastFailureAt === null ||
            Date.now() - this.lastFailureAt >= this.config.failureCooldownMs
        ) {
            return true;
        }

        return false;
    }

    markUnavailable(): void {
        this.available = false;

        this.lastFailureAt = Date.now();
    }

    markHealthy(): void {
        this.available = true;

        this.lastFailureAt = null;
    }

    async health(): Promise<MlHealthResponse> {
        const data = await this.get<MlHealthResponse>(HEALTH_ENDPOINT);

        if (data.status === 'ok' && data.model_loaded) {
            this.markHealthy();
        }

        return data;
    }

    async predict(request: MlPredictRequest): Promise<MlPredictionResponse> {
        return this.post<MlPredictionResponse>(
            PREDICT_ENDPOINT,

            request,
        );
    }

    async recommend(request: MlRecommendRequest): Promise<MlRecommendResponse> {
        return this.post<MlRecommendResponse>(RECOMMEND_ENDPOINT, request);
    }

    private async post<TResponse>(
        endpoint: string,

        body: unknown,
    ): Promise<TResponse> {
        try {
            const response: AxiosResponse<TResponse> = await firstValueFrom(
                this.http
                    .post<TResponse>(this.url(endpoint), body, {
                        timeout: this.config.timeoutMs,
                    })
                    .pipe(timeout(this.config.timeoutMs))
                    .pipe(
                        catchError((error: AxiosError) =>
                            throwError(() => error),
                        ),
                    ),
            );

            this.markHealthy();

            return response.data;
        } catch (error) {
            this.markUnavailable();

            this.logger.warn(
                `ML call to ${endpoint} failed: ${this.describe(error)}`,
            );

            throw error;
        }
    }

    private async get<TResponse>(endpoint: string): Promise<TResponse> {
        try {
            const response: AxiosResponse<TResponse> = await firstValueFrom(
                this.http
                    .get<TResponse>(this.url(endpoint), {
                        timeout: this.config.timeoutMs,
                    })
                    .pipe(timeout(this.config.timeoutMs))
                    .pipe(
                        catchError((error: AxiosError) =>
                            throwError(() => error),
                        ),
                    ),
            );

            return response.data;
        } catch (error) {
            this.markUnavailable();

            throw error;
        }
    }

    private url(endpoint: string): string {
        return `${this.config.baseUrl.replace(/\/$/, '')}${endpoint}`;
    }

    private describe(error: unknown): string {
        if (error instanceof AxiosError) {
            if (error.response) {
                return `HTTP ${error.response.status}`;
            }

            return error.code ?? error.message;
        }

        return String(error);
    }
}
