import {createRoot} from 'react-dom/client';

import '../styles/index.css';

import {TooltipProvider} from '@/components/ui/tooltip';
import EmbeddedAutomationHubApp from '@/ee/EmbeddedAutomationHubApp';
import AutomationHubLayout from '@/ee/pages/embedded/automation-hub/AutomationHubLayout';
import HubBuilderView from '@/ee/pages/embedded/automation-hub/HubBuilderView';
import RequireTab from '@/ee/pages/embedded/automation-hub/RequireTab';
import AutomationsView from '@/ee/pages/embedded/automation-hub/views/AutomationsView';
import ConnectionsView from '@/ee/pages/embedded/automation-hub/views/ConnectionsView';
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
                children: [
                    {
                        element: (
                            <RequireTab tab="automations">
                                <AutomationsView />
                            </RequireTab>
                        ),
                        index: true,
                    },
                    {
                        element: (
                            <RequireTab tab="connections">
                                <ConnectionsView />
                            </RequireTab>
                        ),
                        path: 'connections',
                    },
                    {element: <HubBuilderView />, path: 'builder/:workflowUuid'},
                ],
                element: <AutomationHubLayout />,
                path: 'hub',
            },
        ],
        element: <EmbeddedAutomationHubApp />,
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
