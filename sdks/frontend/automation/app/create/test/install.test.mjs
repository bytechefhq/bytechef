import {describe, expect, it} from 'vitest';

import {installCommand} from '../src/install.mjs';

describe('installCommand', () => {
    it.each([
        ['npm', 'npm', ['install']],
        ['pnpm', 'pnpm', ['install']],
        ['yarn', 'yarn', ['install']],
        ['bun', 'bun', ['install']],
    ])('%s', (packageManager, command, args) => {
        expect(installCommand(packageManager, 'linux')).toEqual({args, command, shell: false});
        expect(installCommand(packageManager, 'win32')).toEqual({args, command, shell: true});
    });
});
