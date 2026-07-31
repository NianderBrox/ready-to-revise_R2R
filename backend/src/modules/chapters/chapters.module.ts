import { Module } from '@nestjs/common';
import { PrismaModule } from '../../prisma/prisma.module';
import { ChaptersService } from './application/services/chapters.service';
import { ChaptersRepository } from './infrastructure/repositories/chapters.repository';
import { ChaptersController } from './presentation/controllers/chapters.controller';

@Module({
    imports: [PrismaModule],
    controllers: [ChaptersController],
    providers: [ChaptersService, ChaptersRepository],
    exports: [ChaptersRepository],
})
export class ChaptersModule {}
