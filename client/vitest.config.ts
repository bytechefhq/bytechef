import * as path from 'node:path';
import {defineConfig} from 'vitest/config';

export default defineConfig({
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    test: {
        coverage: {
            exclude: ['.vitest/', 'node_modules/', 'src/middleware', '**/*.test.ts', '**/*.test.tsx'],
            reporter: ['html', 'lcov', 'text'],
        },
        environment: 'jsdom',
        exclude: ['node_modules', 'test/playwright/**'],
        globals: true,
        setupFiles: '.vitest/setup.ts',
        // Raised from vitest's 5000ms default for two reasons, both about the whole test body rather than any
        // single query:
        //
        // 1. A `waitFor`/`findBy` timeout can never exceed the test's own budget. The suite already carries a
        //    {timeout: 10000} query, which under a 5000ms test timeout could never reach its own deadline —
        //    the body died first with a generic "Test timed out", hiding testing-library's diagnostic.
        // 2. userEvent-driven Radix dialog specs run ~800ms in isolation and slow roughly 6x under full-suite
        //    CPU contention, which put them right on the 5000ms line: they failed in whichever file happened
        //    to lose the scheduling race, in runs testing something else entirely.
        //
        // 15000 clears (1) and leaves ~3x headroom over the observed contention factor in (2). It does not
        // mask a hung test — that still fails, just 10s later, and only a hanging test pays the difference.
        testTimeout: 15000,
    },
});
