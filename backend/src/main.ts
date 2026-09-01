import 'reflect-metadata';
import { Logger, ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { ResponseInterceptor } from './common/interceptors/response.interceptor';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';
import { ConfigService } from '@nestjs/config';

const logger = new Logger('Bootstrap');

async function bootstrap() {
    const app = await NestFactory.create(AppModule, {
        bufferLogs: true,
    });

    /*
     * Global API Prefix
     * Example:
     * /api/v1/auth/login
     */
    app.setGlobalPrefix('api/v1');

    const configService = app.get(ConfigService);

    /*
     * Enable CORS
     */
    const corsOrigin = configService.get<string>('CORS_ORIGIN', '*');

    app.enableCors({ origin: corsOrigin });

    /*
     * Global Validation
     */
    app.useGlobalPipes(
        new ValidationPipe({
            whitelist: true,
            transform: true,
            forbidNonWhitelisted: true,
        }),
    );

    app.useGlobalInterceptors(new ResponseInterceptor());

    app.useGlobalFilters(new HttpExceptionFilter());

    /*
     * Swagger Configuration
     */
    const config = new DocumentBuilder()
        .setTitle('Ready to Revise API')
        .setDescription('Backend API for Ready to Revise')
        .setVersion('1.0')
        .build();

    const document = SwaggerModule.createDocument(app, config);

    SwaggerModule.setup('api/docs', app, document);

    const port = configService.get<number>('PORT', 3000);

    await app.listen(port);

    logger.log(`🚀 Server running on http://localhost:${port}`);

    logger.log(`📖 Swagger: http://localhost:${port}/api/docs`);
}

bootstrap();
