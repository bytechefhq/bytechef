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
            exclude: ['.vitest/', 'node_modules/', 'src/middleware', '**/*.test.tsx'],
            reporter: ['html', 'lcov', 'text'],
        },
        environment: 'jsdom',
        exclude: ['node_modules', 'test/playwright/**'],
        globals: true,
        setupFiles: '.vitest/setup.ts',
    },
});
