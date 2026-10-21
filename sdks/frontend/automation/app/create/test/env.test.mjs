import {describe, expect, it} from 'vitest';

import {
    buildEnvLocal,
    detectPackageManager,
    generateSessionSecret,
    nodeVersionWarning,
    resolveTarget,
    validatePackageName,
} from '../src/env.mjs';

const SECRET = 'a'.repeat(64);

describe('buildEnvLocal', () => {
    it('takes the url from the flag and the key from the environment', () => {
        expect(
            buildEnvLocal({env: {BYTECHEF_APP_KEY: 'sk-1'}, sessionSecret: SECRET, url: 'https://bc.example'})
        ).toEqual({
            content: `BYTECHEF_URL=https://bc.example\nBYTECHEF_APP_KEY=sk-1\nBYTECHEF_SESSION_SECRET=${SECRET}\n`,
            keyFromEnvironment: true,
        });
    });

    it('falls back to the environment url and leaves both empty otherwise', () => {
        expect(buildEnvLocal({env: {BYTECHEF_URL: 'https://env.example'}, sessionSecret: SECRET})).toEqual({
            content: `BYTECHEF_URL=https://env.example\nBYTECHEF_APP_KEY=\nBYTECHEF_SESSION_SECRET=${SECRET}\n`,
            keyFromEnvironment: false,
        });
        expect(buildEnvLocal({env: {}, sessionSecret: SECRET})).toEqual({
            content: `BYTECHEF_URL=\nBYTECHEF_APP_KEY=\nBYTECHEF_SESSION_SECRET=${SECRET}\n`,
            keyFromEnvironment: false,
        });
    });

    it('generates a new session secret by default and never takes one from the environment', () => {
        const first = buildEnvLocal({env: {BYTECHEF_SESSION_SECRET: 'from-the-shell'}});
        const second = buildEnvLocal({env: {}});
        const secretOf = (content) => /^BYTECHEF_SESSION_SECRET=(.*)$/m.exec(content)[1];

        expect(first.content).not.toContain('from-the-shell');
        expect(secretOf(first.content)).toMatch(/^[A-Za-z0-9_-]{64}$/);
        expect(secretOf(first.content)).not.toBe(secretOf(second.content));
    });
});

describe('generateSessionSecret', () => {
    it('encodes 48 random bytes as base64url', () => {
        const requested = [];
        const secret = generateSessionSecret((size) => {
            requested.push(size);

            return Buffer.alloc(size, 0xff);
        });

        expect(requested).toEqual([48]);
        expect(secret).toBe('_'.repeat(64));
    });

    it('gives at least 32 characters and a different value each time', () => {
        const secrets = new Set(Array.from({length: 20}, () => generateSessionSecret()));

        expect(secrets.size).toBe(20);

        for (const secret of secrets) {
            expect(secret.length).toBeGreaterThanOrEqual(32);
        }
    });
});

describe('detectPackageManager', () => {
    it.each([
        ['pnpm/9.0.0 node/v22.0.0', 'pnpm'],
        ['yarn/4.0.0 npm/? node/v22.0.0', 'yarn'],
        ['bun/1.1.0', 'bun'],
        ['npm/11.0.0 node/v22.0.0', 'npm'],
        [undefined, 'npm'],
    ])('%s → %s', (agent, expected) => {
        expect(detectPackageManager({npm_config_user_agent: agent})).toBe(expected);
    });
});

describe('validatePackageName', () => {
    it('accepts a valid name and rejects invalid ones with the rule', () => {
        expect(validatePackageName('my-app')).toBe('my-app');
        expect(() => validatePackageName('My App')).toThrowError(/lower-case/);
        expect(() => validatePackageName('.hidden')).toThrowError(/start/);
    });
});

describe('nodeVersionWarning', () => {
    it.each([
        ['22.17.1', true],
        ['22.18.0', false],
        ['22.23.3', false],
        ['24.15.0', false],
        ['20.19.0', true],
    ])('%s warns: %s', (version, warns) => {
        expect(nodeVersionWarning(version) !== null).toBe(warns);
    });

    it('names the minimum version', () => {
        expect(nodeVersionWarning('22.17.1')).toMatch(/22\.18\.0/);
    });
});

describe('buildEnvLocal line breaks', () => {
    it.each(['sk-1\nINJECT=1', 'sk-1\rINJECT=1', 'sk-1\r\n'])('refuses the key %j without echoing it', (key) => {
        expect(() => buildEnvLocal({env: {BYTECHEF_APP_KEY: key}, url: 'https://bc.example'})).toThrowError(
            'BYTECHEF_APP_KEY must not contain a line break'
        );
    });

    it('refuses a line break in the URL from the flag or the environment', () => {
        expect(() => buildEnvLocal({env: {}, url: 'https://bc.example\rX=1'})).toThrowError(
            'The ByteChef URL must not contain a line break'
        );
        expect(() => buildEnvLocal({env: {BYTECHEF_URL: 'https://bc.example\nX=1'}})).toThrowError(
            'The ByteChef URL must not contain a line break'
        );
    });
});

describe('scoped package names', () => {
    it('accepts @scope/name and checks both parts', () => {
        expect(validatePackageName('@acme/my-app')).toBe('@acme/my-app');
        expect(() => validatePackageName('@Acme/my-app')).toThrowError(/lower-case/);
        expect(() => validatePackageName('@acme/')).toThrowError(/start/);
        expect(() => validatePackageName('@/my-app')).toThrowError(/start/);
    });

    it('refuses a name longer than 214 characters, scope included', () => {
        expect(() => validatePackageName(`@acme/${'a'.repeat(209)}`)).toThrowError(/214/);
    });
});

describe('resolveTarget', () => {
    it('creates name/ for @scope/name and keeps the scope in the package name', () => {
        expect(resolveTarget('@acme/my-app', '/work')).toEqual({
            directory: 'my-app',
            packageName: '@acme/my-app',
            targetDir: '/work/my-app',
        });
    });

    it('treats trailing slashes on a scoped name like the bare scoped name', () => {
        const expected = {directory: 'my-app', packageName: '@acme/my-app', targetDir: '/work/my-app'};

        expect(resolveTarget('@acme/my-app/', '/work')).toEqual(expected);
        expect(resolveTarget('@acme/my-app///', '/work')).toEqual(expected);
    });

    it('still refuses a scope without a name', () => {
        expect(() => resolveTarget('@acme/', '/work')).toThrow(/not a valid package name/);
    });

    it('takes the package name from the directory otherwise', () => {
        expect(resolveTarget('my-app', '/work')).toEqual({
            directory: 'my-app',
            packageName: 'my-app',
            targetDir: '/work/my-app',
        });
        expect(resolveTarget('.', '/work/dot')).toEqual({directory: '.', packageName: 'dot', targetDir: '/work/dot'});
        expect(resolveTarget('apps/@acme/web', '/work')).toEqual({
            directory: 'apps/@acme/web',
            packageName: 'web',
            targetDir: '/work/apps/@acme/web',
        });
    });
});
