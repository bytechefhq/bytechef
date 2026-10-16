import {getConnectedUserExternalId} from '@/ee/pages/embedded/workflow-executions/utils/getConnectedUserExternalId';
import {describe, expect, it} from 'vitest';

describe('getConnectedUserExternalId', () => {
    it('returns the external id of the connected user own deployment', () => {
        expect(getConnectedUserExternalId('__EMBEDDED__user-42')).toBe('user-42');
    });

    it.each(['DEVELOPMENT', 'STAGING', 'PRODUCTION'])(
        'strips the %s suffix of a reference deployment',
        (environment) => {
            expect(getConnectedUserExternalId(`__EMBEDDED__user-42__${environment}`)).toBe('user-42');
        }
    );

    it('keeps double underscores that belong to the external id', () => {
        expect(getConnectedUserExternalId('__EMBEDDED__acme__user__PRODUCTION')).toBe('acme__user');
        expect(getConnectedUserExternalId('__EMBEDDED__acme__user')).toBe('acme__user');
    });

    it('returns undefined when the name is missing', () => {
        expect(getConnectedUserExternalId(undefined)).toBeUndefined();
        expect(getConnectedUserExternalId('')).toBeUndefined();
    });
});
