import { Module } from '@nestjs/common';
import { StudyItemsController } from './study-items.controller';
import { StudyItemsService } from './study-items.service';
import { StudyItemsRepository } from './study-items.repository';
import { PrismaModule } from '../../prisma/prisma.module';

@Module({
    imports: [PrismaModule],
    controllers: [StudyItemsController],
    providers: [StudyItemsService, StudyItemsRepository],
})
export class StudyItemsModule {}
