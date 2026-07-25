import { NestFactory } from '@nestjs/core';
import { AiTestingModule } from './ai-testing.module';

export async function createAiContext() {
    return NestFactory.createApplicationContext(AiTestingModule);
}
