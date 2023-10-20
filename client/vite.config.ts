import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tsconfigPaths from 'vite-tsconfig-paths'

// https://vitejs.dev/config/
export default defineConfig({
  esbuild: {
    // https://github.com/vitejs/vite/issues/8644#issuecomment-1159308803
    logOverride: { 'this-is-undefined-in-esm': 'silent' }
  },
  optimizeDeps: {
    esbuildOptions: {
      target: 'es2020'
    }
  },
  plugins: [react(), tsconfigPaths()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9555',
        changeOrigin: true,
        secure: false
        // rewrite: (path) => path.replace(/^\/api/, ""),
      }
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: '.vitest/setup.ts',
    coverage: {
      reporter: ['text', 'html'],
      exclude: ['node_modules/', './vitest/setup.ts', '**/*.test.tsx']
    }
  }
})
