import {createRoot} from 'react-dom/client';

import '../styles/index.css';

import {TooltipProvider} from '@/components/ui/tooltip';
import EmbeddedIntegrationMarketplaceApp from '@/ee/EmbeddedIntegrationMarketplaceApp';
import IntegrationMarketplaceGate from '@/ee/pages/embedded/integration-marketplace/IntegrationMarketplaceGate';
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
        children: [{element: <IntegrationMarketplaceGate />, path: 'marketplace'}],
        element: <EmbeddedIntegrationMarketplaceApp />,
        path: '/embedded',
    },
]);

root.render(
    <StrictMode>
        <QueryClientProvider client={queryClient}>
            <ThemeProvider persist={false}>
                <TooltipProvider>
                    <I18n>
                        <RouterProvider router={router} />
                    </I18n>
                </TooltipProvider>
            </ThemeProvider>
        </QueryClientProvider>
    </StrictMode>
);
