import { Logger, ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import {
  DocumentBuilder,
  SwaggerModule,
} from '@nestjs/swagger';

import { AppModule } from './app.module';
const logger = new Logger('Bootstrap');

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  /*
   * Global API Prefix
   * Example:
   * /api/v1/auth/login
   */
  app.setGlobalPrefix('api/v1');

  /*
   * Enable CORS
   */
  app.enableCors();

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

  await app.listen(process.env.PORT ?? 3000);

  logger.log(
    `🚀 Server running on http://localhost:${process.env.PORT ?? 3000}`,
  );

  logger.log(
    `📖 Swagger: http://localhost:${process.env.PORT ?? 3000}/api/docs`,
  );
}

bootstrap();