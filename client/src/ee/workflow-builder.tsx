import {createRoot} from 'react-dom/client';

import '../styles/index.css';

import {TooltipProvider} from '@/components/ui/tooltip';
import EmbeddedWorkflowBuilderApp from '@/ee/EmbeddedWorkflowBuilderApp';
import WorkflowBuilder from '@/ee/pages/embedded/workflow-builder/WorkflowBuilder';
import I18n from '@/i18n';
import {ThemeProvider} from '@/shared/providers/theme-provider';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {StrictMode} from 'react';
import {RouterProvider, createHashRouter} from 'react-router-dom';

const container = document.getElementById('root') as HTMLDivElement;
const root = createRoot(container);
const queryClient = new QueryClient();

void applicationInfoStore.getState().getApplicationInfo();

const router = createHashRouter([
    {
        children: [
            {
                element: <WorkflowBuilder />,
                path: 'builder/:workflowUuid',
            },
        ],
        element: <EmbeddedWorkflowBuilderApp />,
        path: '/embedded',
    },
]);

root.render(
    <StrictMode>
        <I18n>
            <ThemeProvider defaultTheme="light" persist={false}>
                <QueryClientProvider client={queryClient}>
                    <TooltipProvider>
                        <RouterProvider router={router} />
                    </TooltipProvider>
                </QueryClientProvider>
            </ThemeProvider>
        </I18n>
    </StrictMode>
);
