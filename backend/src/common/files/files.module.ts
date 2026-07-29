import { Module } from '@nestjs/common';
import { FileMapper } from '../mappers/file.mapper';
// import { FileMapper } from 'src/common/mappers/file.mapper';

@Module({
    providers: [FileMapper],
    exports: [FileMapper],
})
export class FilesModule {}
