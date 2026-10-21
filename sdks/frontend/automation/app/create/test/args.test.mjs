import {describe, expect, it} from 'vitest';

import {parseCliArgs, statusCommand, USAGE} from '../src/args.mjs';

describe('parseCliArgs', () => {
    it('defaults the directory, install and package manager', () => {
        expect(parseCliArgs([], {})).toEqual({
            directory: 'bytechef-app',
            help: false,
            install: true,
            packageManager: undefined,
            url: undefined,
        });
    });

    it('reads the directory, --url, --no-install and --package-manager', () => {
        expect(
            parseCliArgs(['my-app', '--url', 'https://bc.example', '--no-install', '--package-manager', 'pnpm'], {})
        ).toEqual({
            directory: 'my-app',
            help: false,
            install: false,
            packageManager: 'pnpm',
            url: 'https://bc.example',
        });
    });

    it('refuses a second positional and explains the npm -- rule', () => {
        expect(() => parseCliArgs(['my-app', 'https://bc.example'], {})).toThrowError(
            /npm create bytechef-app@latest my-app -- --url/
        );
    });

    it('explains when npm consumed the flags', () => {
        expect(() => parseCliArgs(['my-app'], {npm_config_url: 'true'})).toThrowError(
            /npm took --url as its own option/
        );
        expect(() => parseCliArgs(['my-app'], {npm_config_install: ''})).toThrowError(/npm took --no-install/);
    });

    it('reads --help and -h before anything else', () => {
        expect(parseCliArgs(['--help'], {})).toEqual({help: true});
        expect(parseCliArgs(['-h'], {})).toEqual({help: true});
        expect(parseCliArgs(['one', 'two', '--help'], {npm_config_url: 'true'})).toEqual({help: true});
    });

    it('rejects an unknown package manager and an unknown option', () => {
        expect(() => parseCliArgs(['--package-manager', 'deno'], {})).toThrowError(/package-manager/);
        expect(() => parseCliArgs(['--key', 'x'], {})).toThrowError(/Unknown option/);
    });
    it('does not blame npm when the flag reached the scaffolder or npm left the variable empty', () => {
        expect(parseCliArgs(['my-app', '--url', 'https://bc.example'], {npm_config_url: 'true'}).url).toBe(
            'https://bc.example'
        );
        expect(parseCliArgs(['my-app', '--no-install'], {npm_config_install: ''}).install).toBe(false);
        expect(parseCliArgs(['my-app', '--install'], {npm_config_install: ''}).install).toBe(true);
        expect(parseCliArgs(['my-app'], {npm_config_url: ''}).url).toBeUndefined();
    });

    it('accepts an http or https --url and refuses anything else', () => {
        expect(parseCliArgs(['--url', 'http://localhost:9555'], {}).url).toBe('http://localhost:9555');
        expect(() => parseCliArgs(['--url', 'bc.example'], {})).toThrowError(/--url must be an http or https URL/);
        expect(() => parseCliArgs(['--url', 'ftp://bc.example'], {})).toThrowError(
            /--url must be an http or https URL/
        );
    });

    it('refuses a line break in --url, even one the URL parser would drop', () => {
        expect(() => parseCliArgs(['--url', 'https://bc.example\nX=1'], {})).toThrowError(
            '--url must not contain a line break'
        );
        expect(() => parseCliArgs(['--url', 'https://bc.example\r'], {})).toThrowError(
            '--url must not contain a line break'
        );
    });
});

describe('USAGE', () => {
    it('shows the npm and pnpm forms, the -- rule, every flag and that there is no key flag', () => {
        expect(USAGE).toContain('npm create bytechef-app@latest [directory] -- [flags]');
        expect(USAGE).toContain('pnpm create bytechef-app [directory] [flags]');
        expect(USAGE).toContain('flags must follow "--"');

        for (const flag of ['--url <url>', '--no-install', '--package-manager <manager>', '-h, --help']) {
            expect(USAGE).toContain(flag);
        }

        expect(USAGE).toContain('There is no flag for the app key');
    });
});

describe('statusCommand', () => {
    it('uses the npm-only -s flag for npm alone', () => {
        expect(statusCommand('npm')).toBe('npm run -s bytechef:status');
        expect(statusCommand('pnpm')).toBe('pnpm run bytechef:status');
        expect(statusCommand('yarn')).toBe('yarn run bytechef:status');
        expect(statusCommand('bun')).toBe('bun run bytechef:status');
    });
});
