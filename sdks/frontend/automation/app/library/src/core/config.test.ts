import {afterEach, describe, expect, it, vi} from 'vitest';

import {DEFAULT_TIMEOUT_MS, joinUrl, resolveConfig} from './config';

describe('resolveConfig', () => {
    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it('reads BYTECHEF_URL and BYTECHEF_APP_KEY from the environment', () => {
        vi.stubEnv('BYTECHEF_URL', 'https://bc.example/');
        vi.stubEnv('BYTECHEF_APP_KEY', 'key-1');

        const config = resolveConfig({});

        expect(config.baseUrl).toBe('https://bc.example');
        expect(config.appKey).toBe('key-1');
        expect(config.timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
    });

    it('prefers explicit options over the environment', () => {
        vi.stubEnv('BYTECHEF_URL', 'https://env.example');
        vi.stubEnv('BYTECHEF_APP_KEY', 'env-key');

        const config = resolveConfig({appKey: 'opt-key', timeoutMs: 5, url: 'https://opt.example'});

        expect(config).toMatchObject({appKey: 'opt-key', baseUrl: 'https://opt.example', timeoutMs: 5});
    });

    it('fails with a message naming the variable and where a key comes from when the key is missing', () => {
        vi.stubEnv('BYTECHEF_URL', 'https://bc.example');
        vi.stubEnv('BYTECHEF_APP_KEY', '');

        expect(() => resolveConfig({})).toThrowError(/BYTECHEF_APP_KEY.*Rotate key/s);
    });

    it('fails when the url is missing', () => {
        vi.stubEnv('BYTECHEF_URL', '');
        vi.stubEnv('BYTECHEF_APP_KEY', 'k');

        expect(() => resolveConfig({})).toThrowError(/BYTECHEF_URL/);
    });

    it.each([0, -1, Number.NaN, Number.POSITIVE_INFINITY])('rejects timeoutMs %s', (timeoutMs) => {
        expect(() => resolveConfig({appKey: 'k', timeoutMs, url: 'https://bc.example'})).toThrowError(
            'timeoutMs must be a positive number of milliseconds'
        );
    });

    it('never reads VITE_ prefixed variables', () => {
        vi.stubEnv('VITE_BYTECHEF_APP_KEY', 'leaked');
        vi.stubEnv('BYTECHEF_URL', 'https://bc.example');
        vi.stubEnv('BYTECHEF_APP_KEY', '');

        expect(() => resolveConfig({})).toThrowError(/BYTECHEF_APP_KEY/);
    });
});

describe('joinUrl', () => {
    it.each([
        ['https://x', '/api/app/v1/tools', 'https://x/api/app/v1/tools'],
        ['https://x/', '/api/app/v1/tools', 'https://x/api/app/v1/tools'],
        ['https://x/bytechef', '/api/app/v1/tools', 'https://x/bytechef/api/app/v1/tools'],
        ['https://x/bytechef/', '/api/app/v1/tools', 'https://x/bytechef/api/app/v1/tools'],
    ])('joins %s + %s', (base, path, expected) => {
        expect(joinUrl(base, path)).toBe(expected);
    });
});
