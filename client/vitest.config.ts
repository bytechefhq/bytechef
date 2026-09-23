import * as path from 'node:path';
import {defineConfig} from 'vitest/config';

export default defineConfig({
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            'monaco-editor/esm/vs/editor/editor.worker.js': path.resolve(
                __dirname,
                'node_modules/monaco-editor/esm/vs/editor/editor.worker.js'
            ),
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
        server: {
            deps: {
                inline: ['monaco-worker-manager', 'monaco-yaml'],
            },
        },
        setupFiles: '.vitest/setup.ts',
    },
});
