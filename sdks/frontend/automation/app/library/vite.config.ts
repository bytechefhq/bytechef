import {resolve} from 'node:path';
import dts from 'vite-plugin-dts';
import {defineConfig} from 'vitest/config';

export default defineConfig({
    build: {
        lib: {
            entry: {
                'ai-sdk': resolve(import.meta.dirname, 'src/ai-sdk.ts'),
                index: resolve(import.meta.dirname, 'src/index.ts'),
                'server-fns': resolve(import.meta.dirname, 'src/server-fns.ts'),
                'tanstack-start': resolve(import.meta.dirname, 'src/tanstack-start.ts'),
            },
            formats: ['es'],
        },
        minify: false,
        rollupOptions: {
            external: ['@tanstack/react-start', 'ai', /^node:/],
        },
        sourcemap: true,
        target: 'es2022',
    },
    plugins: [
        dts({
            exclude: ['src/**/*.test.ts', 'src/test'],
            tsconfigPath: 'tsconfig.json',
        }),
    ],
    test: {
        coverage: {
            include: ['src/core/**', 'src/tanstack/**', 'src/ai/**'],
            provider: 'v8',
        },
        environment: 'node',
        include: ['src/**/*.test.ts'],
    },
});
