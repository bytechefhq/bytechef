import {lingui} from '@lingui/vite-plugin';
import babelPlugin from '@rolldown/plugin-babel';
import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import * as path from 'node:path';
import {defineConfig} from 'vite';
import {viteSingleFile} from 'vite-plugin-singlefile';
import svgr from 'vite-plugin-svgr';

export default defineConfig({
    base: '',
    build: {
        cssCodeSplit: false,
        emptyOutDir: true,
        outDir: 'dist-mcp-apps/workflow-editor',
        rollupOptions: {
            input: path.resolve(import.meta.dirname, 'mcp-workflow-editor.html'),
        },
        target: 'es2022',
    },
    plugins: [
        react(),
        tailwindcss(),
        babelPlugin({
            plugins: ['@lingui/babel-plugin-lingui-macro'],
        }),
        lingui(),
        svgr(),
        viteSingleFile(),
    ],
    publicDir: false,
    resolve: {
        alias: {
            '@': path.resolve(import.meta.dirname, './src'),
            '@bytechef/embedded-api': path.resolve(import.meta.dirname, '../sdks/api/typescript/src/configuration/index.ts'),
            '@dagrejs/dagre': path.resolve(import.meta.dirname, 'node_modules/@dagrejs/dagre/dist/dagre.cjs'),
        },
        dedupe: ['react', 'react-dom'],
        tsconfigPaths: true,
    },
});
