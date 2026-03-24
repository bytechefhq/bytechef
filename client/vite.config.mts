import babelPlugin from '@rolldown/plugin-babel';
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
            rollupOptions: {
                input: {
                    connect: resolve(__dirname, 'connect.html'),
                    main: resolve(__dirname, 'index.html'),
                },
                output: {
                    manualChunks(id) {
                        if (id.includes('posthog-js')) {
                            return 'vendor-analytics';
                        }

                        if (id.includes('@tanstack/react-query')) {
                            return 'vendor-query';
                        }

                        if (id.includes('/react/') || id.includes('/react-dom/') || id.includes('/react-router-dom/')) {
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
            babelPlugin({
                plugins: ['@lingui/babel-plugin-lingui-macro'],
            }),
            lingui(),
            svgr(),
            isHttps() && basicSsl(),
        ],
        resolve: {
            tsconfigPaths: true,
            alias: {
                '@': path.resolve(__dirname, './src'),
                '@bytechef/embedded-react': path.resolve(__dirname, '../sdks/frontend/embedded/library/react/src/main.ts'),
                '@dagrejs/dagre': path.resolve(__dirname, 'node_modules/@dagrejs/dagre/dist/dagre.cjs.js'),
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
