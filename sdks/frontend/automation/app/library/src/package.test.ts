import {readFileSync} from 'node:fs';

import {describe, expect, it} from 'vitest';

const packageJson = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as {
    exports: Record<string, unknown>;
    peerDependencies: Record<string, string>;
    peerDependenciesMeta: Record<string, {optional?: boolean}>;
};

describe('package.json', () => {
    it('keeps @tanstack/react-start as an optional peer, which makes Start compile the shipped server functions', () => {
        expect(packageJson.peerDependencies['@tanstack/react-start']).toBeDefined();
        expect(packageJson.peerDependenciesMeta['@tanstack/react-start']?.optional).toBe(true);
    });

    it('keeps ai as an optional peer', () => {
        expect(packageJson.peerDependencies['ai']).toBeDefined();
        expect(packageJson.peerDependenciesMeta['ai']?.optional).toBe(true);
    });

    it('exports exactly the four entry points and package.json', () => {
        expect(Object.keys(packageJson.exports).sort()).toEqual([
            '.',
            './ai-sdk',
            './package.json',
            './tanstack-start',
            './tanstack-start/server-fns',
        ]);
    });
});
