import React from 'react';
import {createRoot} from 'react-dom/client';

import {worker} from './mocks/server';

import './styles/index.css';

import {ThemeProvider} from '@/providers/theme-provider';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactQueryDevtools} from '@tanstack/react-query-devtools';
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import YamlWorker from 'monaco-yaml/yaml.worker?worker';
import {RouterProvider} from 'react-router-dom';

import {router} from './routes';

window.MonacoEnvironment = {
    getWorker(moduleId: string, label: string) {
        switch (label) {
            case 'editorWorkerService':
                return new EditorWorker();
            case 'javascript':
                return new TsWorker();
            case 'json':
                return new JsonWorker();
            case 'yaml':
                return new YamlWorker();
            default:
                throw new Error(`Unknown label ${label} for moduleId ${moduleId}`);
        }
    },
};

if (process.env.NODE_ENV === 'mock') {
    worker.start().then(() => renderApp());
} else {
    renderApp();
}

function renderApp() {
    const container = document.getElementById('root') as HTMLDivElement;
    const root = createRoot(container);
    const queryClient = new QueryClient();

    root.render(
        <React.StrictMode>
            <ThemeProvider defaultTheme="light">
                <QueryClientProvider client={queryClient}>
                    <RouterProvider router={router} />

                    <ReactQueryDevtools buttonPosition="bottom-right" initialIsOpen={false} />
                </QueryClientProvider>
            </ThemeProvider>
        </React.StrictMode>
    );
}
