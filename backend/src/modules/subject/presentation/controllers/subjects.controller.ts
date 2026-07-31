import {
    Body,
    Controller,
    Delete,
    Get,
    Param,
    Patch,
    Post,
} from '@nestjs/common';
import { CreateSubjectDto } from '../dto/create-subject.dto';
import { UpdateSubjectDto } from '../dto/update-subject.dto';
import { ParseUUIDPipe } from '@nestjs/common';
import { SubjectsService } from '../../application/services/subjects.service';

@Controller('subjects')
export class SubjectsController {
    constructor(private readonly subjectsService: SubjectsService) {}

    @Post()
    async create(@Body() dto: CreateSubjectDto) {
        return this.subjectsService.create(dto);
    }

    @Get()
    async findAll() {
        return this.subjectsService.findAll();
    }

    @Get(':id')
    async findOne(@Param('id', ParseUUIDPipe) id: string) {
        return this.subjectsService.findOne(id);
    }

    @Patch(':id')
    async update(
        @Param('id', ParseUUIDPipe) id: string,
        @Body() dto: UpdateSubjectDto,
    ) {
        return this.subjectsService.update(id, dto);
    }

    @Delete(':id')
    async remove(@Param('id', ParseUUIDPipe) id: string) {
        await this.subjectsService.remove(id);

        return {
            success: true,
            message: 'Subject deleted successfully.',
        };
    }
}
