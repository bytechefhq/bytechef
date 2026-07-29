import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';
import {viteSingleFile} from 'vite-plugin-singlefile';

// The widget must ship as ONE self-contained HTML file: MCP App hosts fetch a single
// ui:// resource and render it in a sandboxed iframe, so there is no place to serve
// secondary chunks or assets from.
export default defineConfig({
    base: '',
    build: {
        cssCodeSplit: false,
        target: 'es2022',
    },
    plugins: [react(), viteSingleFile()],
});
