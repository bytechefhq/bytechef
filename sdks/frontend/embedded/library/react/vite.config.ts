/// <reference types="vite/client" />
import path, {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {globSync} from 'glob';
import {defineConfig} from 'vitest/config';
import react from '@vitejs/plugin-react';
import dts from 'vite-plugin-dts';
import {libInjectCss} from 'vite-plugin-lib-inject-css';

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        react(),
        // https://github.com/vitejs/vite/issues/1579#issuecomment-1483756199
        libInjectCss(),
        dts({
            exclude: ['**/*.stories.tsx', 'src/test', '**/*.test.tsx'],
            tsconfigPath: 'tsconfig.app.json',
        }),
    ],
    build: {
        lib: {
            entry: resolve(__dirname, 'src/main.ts'),
            formats: ['es'],
        },
        rollupOptions: {
            external: ['react', 'react-dom', 'react-dom/client', 'react/jsx-runtime'],
            // https://rollupjs.org/configuration-options/#input
            input: Object.fromEntries(
                globSync(['src/components/**/index.{ts,tsx}', 'src/main.ts'])
                    .filter((file) => !file.includes('.test.'))
                    .map((file) => {
                        // This removes `src/` as well as the file extension from each
                        // file, so e.g. src/nested/foo.js becomes nested/foo
                        const entryName = path.relative('src', file.slice(0, file.length - path.extname(file).length));
                        // This expands the relative paths to absolute paths, so e.g.
                        // src/nested/foo becomes /project/src/nested/foo.js
                        const entryUrl = fileURLToPath(new URL(file, import.meta.url));
                        return [entryName, entryUrl];
                    })
            ),
            output: {
                entryFileNames: '[name].js',
                assetFileNames: 'assets/[name][extname]',
                globals: {
                    react: 'React',
                    'react-dom': 'ReactDOM',
                },
            },
        },
    },
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
        css: true,
        coverage: {
            provider: 'v8',
            include: ['src/components', 'src/hooks', 'src/utils'],
            exclude: ['**/*.stories.tsx', '**/*.test.tsx'],
        },
    },
});
