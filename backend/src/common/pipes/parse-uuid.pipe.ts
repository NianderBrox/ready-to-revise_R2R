import { BadRequestException, Injectable, PipeTransform } from '@nestjs/common';

const UUID_REGEX =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

@Injectable()
export class ParseUuidPipe implements PipeTransform<string> {
    transform(value: string): string {
        if (!UUID_REGEX.test(value)) {
            throw new BadRequestException('Invalid UUID.');
        }

        return value;
    }
}
