import babelPlugin from '@rolldown/plugin-babel';
import tailwindcss from '@tailwindcss/vite';
import basicSsl from '@vitejs/plugin-basic-ssl';
import react from '@vitejs/plugin-react';
import * as path from 'node:path';
import {resolve} from 'node:path';
import {defineConfig, loadEnv} from 'vite';
import svgr from 'vite-plugin-svgr';
import { lingui } from '@lingui/vite-plugin';

// https://vitejs.dev/config/
export default ({mode}) => {
    // Make Vite env vars available.
    // https://stackoverflow.com/a/66389044
    process.env = {...process.env, ...loadEnv(mode, process.cwd())};

    const isHttps = () => process.env.VITE_HTTPS === 'true';

    return defineConfig({
        build: {
            manifest: true,
            rollupOptions: {
                input: {
                    connect: resolve(import.meta.dirname, 'connect.html'),
                    main: resolve(import.meta.dirname, 'index.html'),
                    workflowBuilder: resolve(import.meta.dirname, 'workflow-builder.html'),
                },
                output: {
                    manualChunks(id) {
                        if (id.includes('@tanstack/react-query')) {
                            return 'vendor-query';
                        }

                        if (
                            id.includes('/node_modules/react/') ||
                            id.includes('/node_modules/react-dom/') ||
                            id.includes('/node_modules/react-router-dom/')
                        ) {
                            return 'vendor-react';
                        }

                        if (id.includes('@radix-ui/react-icons') || id.includes('lucide-react')) {
                            return 'vendor-ui';
                        }
                    },
                },
            },
        },
        plugins: [
            react(),
            tailwindcss(),
            babelPlugin({
                plugins: ['@lingui/babel-plugin-lingui-macro'],
            }),
            lingui(),
            svgr(),
            isHttps() && basicSsl(),
        ],
        resolve: {
            dedupe: ['react', 'react-dom'],
            tsconfigPaths: true,
            alias: {
                '@': path.resolve(import.meta.dirname, './src'),
                '@bytechef/embedded': path.resolve(import.meta.dirname, '../sdks/frontend/embedded/library/src/main.ts'),
                '@dagrejs/dagre': path.resolve(import.meta.dirname, 'node_modules/@dagrejs/dagre/dist/dagre.cjs'),
            },
        },
        server: {
            host: '127.0.0.1',
            proxy: {
                '/actuator': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
                '/api': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
                '/approvals': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
                '/callback': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
                '/graphql': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
                '/icons': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
                '/job/resume': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                },
                '/webhooks': {
                    changeOrigin: true,
                    secure: false,
                    target: 'http://localhost:9555',
                    // rewrite: (path) => path.replace(/^\/api/, ""),
                },
            },
        },
    });
};
