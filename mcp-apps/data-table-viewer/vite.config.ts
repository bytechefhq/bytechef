import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';
import {viteSingleFile} from 'vite-plugin-singlefile';

export default defineConfig({
    base: '',
    build: {
        cssCodeSplit: false,
        target: 'es2022',
    },
    plugins: [react(), viteSingleFile()],
});
