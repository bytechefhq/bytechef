import {createRoot} from 'react-dom/client';

import './styles/index.css';

import './styles/components.css';

import {TooltipProvider} from '@/components/ui/tooltip';
import {getRouter as getEmbeddedRouter} from '@/embeddedWorkflowBuilderRoutes';
import {ConditionalPostHogProvider} from '@/shared/providers/conditional-posthog-provider';
import {ThemeProvider} from '@/shared/providers/theme-provider';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactQueryDevtools} from '@tanstack/react-query-devtools';
import EditorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import JsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import TsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import YamlWorker from 'monaco-yaml/yaml.worker?worker';
import {StrictMode} from 'react';
import {RouterProvider} from 'react-router-dom';

import {getRouter as getMainRouter} from './routes';

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
    import('./mocks/server').then(({worker}) => {
        worker.start().then(() => renderApp());
    });
} else {
    renderApp();
}

const publicRoutes = ['/activate', '/login', '/register', '/password-reset', '/password-reset/finish', '/verify-email'];

async function renderApp() {
    const container = document.getElementById('root') as HTMLDivElement;
    const root = createRoot(container);
    const queryClient = new QueryClient();

    const isEmbeddedWorkflowBuilder = window.location.pathname.includes('/embedded/workflow-builder');

    const router = isEmbeddedWorkflowBuilder ? getEmbeddedRouter() : getMainRouter(queryClient);

    await applicationInfoStore.getState().getApplicationInfo();

    if (!publicRoutes.includes(window.location.pathname) && !authenticationStore.getState().sessionHasBeenFetched) {
        const result = await authenticationStore.getState().getAccount();

        if (!result && window.location.pathname !== '/login') {
            window.location.pathname = '/login';
        }
    }

    root.render(
        // <StrictMode>
        <ThemeProvider defaultTheme="light">
            <QueryClientProvider client={queryClient}>
                <ConditionalPostHogProvider>
                    <TooltipProvider>
                        <RouterProvider router={router} />
                    </TooltipProvider>
                </ConditionalPostHogProvider>

                <ReactQueryDevtools buttonPosition="bottom-right" initialIsOpen={false} />
            </QueryClientProvider>
        </ThemeProvider>
        // </StrictMode>
    );
}
