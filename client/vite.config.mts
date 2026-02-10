import basicSsl from '@vitejs/plugin-basic-ssl';
import react from '@vitejs/plugin-react';
import * as path from 'node:path';
import {defineConfig, loadEnv} from 'vite';
import svgr from 'vite-plugin-svgr';
import tsconfigPaths from 'vite-tsconfig-paths';
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
                output: {
                    manualChunks: {
                        'vendor-analytics': ['posthog-js'],
                        'vendor-d3': ['d3-hierarchy', 'd3-timer'],
                        'vendor-dagre': ['@dagrejs/dagre'],
                        'vendor-editor': ['monaco-editor', '@monaco-editor/react'],
                        'vendor-flow': ['@xyflow/react'],
                        'vendor-json': ['react-json-view'],
                        'vendor-query': ['@tanstack/react-query'],
                        'vendor-react': ['react', 'react-dom', 'react-router-dom'],
                        'vendor-ui': ['@radix-ui/react-icons', 'lucide-react'],
                    },
                },
            },
        },
        esbuild: {
            // https://github.com/vitejs/vite/issues/8644#issuecomment-1159308803
            logOverride: {'this-is-undefined-in-esm': 'silent'},
        },
        optimizeDeps: {
            esbuildOptions: {
                target: 'es2020',
            },
        },
        plugins: [
            react({
                babel: {
                    plugins: ['@lingui/babel-plugin-lingui-macro'],
                },
            }),
            lingui(),
            tsconfigPaths(),
            svgr(),
            isHttps() && basicSsl(),
        ],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, './src'),
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
