import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { AppModule } from '../../src/app.module';
import { ResponseInterceptor } from '../../src/common/interceptors/response.interceptor';
import { HttpExceptionFilter } from '../../src/common/filters/http-exception.filter';
import { DocumentAnalysisProvider } from '../../src/modules/document-analysis/domain/interfaces/document-analysis.provider';
import { DOCUMENT_ANALYSIS_PROVIDER } from '../../src/modules/document-analysis/infrastructure/tokens/document-analysis-provider.token';

interface CreateTestAppOptions {
    documentAnalysisProvider?: DocumentAnalysisProvider;
}

export async function createTestApp(
    options: CreateTestAppOptions = {},
): Promise<INestApplication> {
    const builder = Test.createTestingModule({
        imports: [AppModule],
    });

    if (options.documentAnalysisProvider) {
        builder
            .overrideProvider(DOCUMENT_ANALYSIS_PROVIDER)
            .useValue(options.documentAnalysisProvider);
    }

    const moduleFixture: TestingModule = await builder.compile();

    const app = moduleFixture.createNestApplication();

    app.setGlobalPrefix('api/v1');

    app.useGlobalPipes(
        new ValidationPipe({
            whitelist: true,
            transform: true,
            forbidNonWhitelisted: true,
        }),
    );

    app.useGlobalInterceptors(new ResponseInterceptor());

    app.useGlobalFilters(new HttpExceptionFilter());

    await app.init();

    return app;
}
