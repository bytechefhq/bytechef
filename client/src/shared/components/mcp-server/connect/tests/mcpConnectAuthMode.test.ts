import {describe, expect, it} from 'vitest';

import {getMcpConnectAuthMode} from '../mcpConnectAuthMode';

describe('getMcpConnectAuthMode', () => {
    it('returns URL_ONLY when authentication is not required, even with OAuth available', () => {
        expect(getMcpConnectAuthMode({authenticationRequired: false, oauthAvailable: false})).toBe('URL_ONLY');
        expect(getMcpConnectAuthMode({authenticationRequired: false, oauthAvailable: true})).toBe('URL_ONLY');
    });

    it('returns API_KEY when authentication is required and OAuth is not available', () => {
        expect(getMcpConnectAuthMode({authenticationRequired: true, oauthAvailable: false})).toBe('API_KEY');
    });

    it('returns API_KEY_OR_OAUTH when authentication is required and OAuth is available', () => {
        expect(getMcpConnectAuthMode({authenticationRequired: true, oauthAvailable: true})).toBe('API_KEY_OR_OAUTH');
    });
});
