import { Module } from '@nestjs/common';
import { StudyItemsController } from './presentation/controllers/study-items.controller';
import { StudyItemsService } from './application/services/study-items.service';
import { PrismaModule } from '../../prisma/prisma.module';
import { StudyItemsRepository } from './infrastructure/repositories/study-items.repository';

@Module({
    imports: [PrismaModule],
    controllers: [StudyItemsController],
    providers: [StudyItemsService, StudyItemsRepository],
    exports: [StudyItemsService],
})
export class StudyItemsModule {}
