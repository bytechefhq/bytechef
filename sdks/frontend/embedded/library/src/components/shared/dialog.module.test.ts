import {readFileSync} from 'node:fs';
import {resolve} from 'node:path';
import {describe, expect, it} from 'vitest';

const css = readFileSync(resolve(__dirname, 'dialog.module.css'), 'utf8');

const OVERRIDABLE_TOKENS = [
    ['--dialog-surface', '--bytechef-dialog-surface'],
    ['--dialog-surface-raised', '--bytechef-dialog-surface-raised'],
    ['--dialog-border', '--bytechef-dialog-border'],
    ['--dialog-text', '--bytechef-dialog-text'],
    ['--dialog-text-muted', '--bytechef-dialog-text-muted'],
    ['--dialog-hover', '--bytechef-dialog-hover'],
    ['--bytechef-blue', '--bytechef-dialog-primary'],
    ['--bytechef-blue-hover', '--bytechef-dialog-primary-hover'],
    ['--dialog-primary-foreground', '--bytechef-dialog-primary-foreground'],
];

describe('dialog.module.css', () => {
    it.each(OVERRIDABLE_TOKENS)('defines %s through the host override %s in every mode block', (token, override) => {
        const definitions = css.match(new RegExp(`${token}:\\s*[^;]+;`, 'g')) ?? [];

        expect(definitions.length).toBeGreaterThan(0);

        for (const definition of definitions) {
            expect(definition).toContain(`var(${override},`);
        }
    });
});
