import {
    getConnectionDialogAllowedClaim,
    resolveConnectionDialogAllowed,
} from '@/ee/pages/embedded/shared/connectionDialogAllowed';
import {describe, expect, it} from 'vitest';

const encodeSegment = (value: object) =>
    btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

const createJwt = (payload: object) => `${encodeSegment({alg: 'RS256'})}.${encodeSegment(payload)}.signature`;

describe('getConnectionDialogAllowedClaim', () => {
    it('allows the connection dialog when there is no token', () => {
        expect(getConnectionDialogAllowedClaim(undefined)).toBe(true);
        expect(getConnectionDialogAllowedClaim(null)).toBe(true);
        expect(getConnectionDialogAllowedClaim('')).toBe(true);
    });

    it('allows the connection dialog when the token has no claim', () => {
        expect(getConnectionDialogAllowedClaim(createJwt({sub: 'user-a'}))).toBe(true);
    });

    it('returns the boolean claim of the token', () => {
        expect(getConnectionDialogAllowedClaim(createJwt({connectionDialogAllowed: false, sub: 'user-a'}))).toBe(false);
        expect(getConnectionDialogAllowedClaim(createJwt({connectionDialogAllowed: true, sub: 'user-a'}))).toBe(true);
    });

    it('ignores a claim that is not a boolean', () => {
        expect(getConnectionDialogAllowedClaim(createJwt({connectionDialogAllowed: 'false', sub: 'user-a'}))).toBe(
            true
        );
    });

    it('allows the connection dialog when the token payload cannot be decoded', () => {
        expect(getConnectionDialogAllowedClaim('header.not-json.signature')).toBe(true);
    });
});

describe('resolveConnectionDialogAllowed', () => {
    const allowedJwt = createJwt({sub: 'user-a'});
    const deniedJwt = createJwt({connectionDialogAllowed: false, sub: 'user-a'});

    it('allows the connection dialog when neither the token nor the parameter restricts it', () => {
        expect(resolveConnectionDialogAllowed({jwtToken: allowedJwt})).toBe(true);
        expect(resolveConnectionDialogAllowed({connectionDialogAllowed: true, jwtToken: allowedJwt})).toBe(true);
    });

    it('lets the deprecated parameter only remove the permission', () => {
        expect(resolveConnectionDialogAllowed({connectionDialogAllowed: false, jwtToken: allowedJwt})).toBe(false);
        expect(resolveConnectionDialogAllowed({connectionDialogAllowed: true, jwtToken: deniedJwt})).toBe(false);
    });
});
