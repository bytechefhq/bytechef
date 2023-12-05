import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';
import svgr from 'vite-plugin-svgr';
import tsconfigPaths from 'vite-tsconfig-paths';

// https://vitejs.dev/config/
export default defineConfig({
    esbuild: {
        // https://github.com/vitejs/vite/issues/8644#issuecomment-1159308803
        logOverride: {'this-is-undefined-in-esm': 'silent'},
    },
    optimizeDeps: {
        esbuildOptions: {
            target: 'es2020',
        },
    },
    plugins: [react(), tsconfigPaths(), svgr()],
    server: {
        host: '127.0.0.1',
        proxy: {
            '/api': {
                changeOrigin: true,
                secure: false,
                target: 'http://localhost:9555',
                // rewrite: (path) => path.replace(/^\/api/, ""),
            },
        },
    },
    test: {
        coverage: {
            exclude: ['.vitest/', 'node_modules/', 'src/middleware', '**/*.test.tsx'],
            reporter: ['html', 'lcov', 'text'],
        },
        environment: 'jsdom',
        globals: true,
        setupFiles: '.vitest/setup.ts',
    },
});
