import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { ConfigService } from '@nestjs/config';
import { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { JwtPayload } from '../../domain/interfaces/jwt-payload.interface';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
    constructor(private readonly configService: ConfigService) {
        super({
            jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),

            ignoreExpiration: false,

            secretOrKey: configService.getOrThrow('JWT_SECRET'),
        });
    }

    async validate(payload: JwtPayload): Promise<CurrentUserData> {
        return {
            userId: payload.sub,
            email: payload.email,
        };
    }
}
