import { createParamDecorator, ExecutionContext } from '@nestjs/common';

import { CurrentUserData } from '../interfaces/current-user-data.interface';

export const CurrentUser = createParamDecorator(
    (_data: unknown, context: ExecutionContext): CurrentUserData => {
        const request = context.switchToHttp().getRequest();

        return request.user as CurrentUserData;
    },
);
