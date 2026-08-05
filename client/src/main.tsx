import {createRoot} from 'react-dom/client';

import './styles/index.css';

import {TooltipProvider} from '@/components/ui/tooltip';
import I18n from '@/i18n';
import {buildLoginPath} from '@/shared/auth/login-redirect-utils';
import {ConditionalPostHogProvider} from '@/shared/providers/conditional-posthog-provider';
import {ThemeProvider} from '@/shared/providers/theme-provider';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactQueryDevtools} from '@tanstack/react-query-devtools';
import {StrictMode} from 'react';
import {RouterProvider} from 'react-router-dom';

import {initUserGuiding} from './hooks/useUserGuiding';

if (process.env.NODE_ENV === 'mock') {
    import('./mocks/server').then(({worker}) => {
        worker.start().then(() => renderApp());
    });
} else {
    renderApp();
}

const publicRoutes = [
    '/activate',
    '/chat',
    '/form',
    '/register',
    '/password-reset',
    '/password-reset/finish',
    '/resume',
    '/verify-email',
];

async function renderApp() {
    const container = document.getElementById('root') as HTMLDivElement;
    const root = createRoot(container);
    const queryClient = new QueryClient();

    const router = (await import('./routes')).getRouter(queryClient);

    await applicationInfoStore.getState().getApplicationInfo();

    if (applicationInfoStore.getState().application?.edition === 'EE') {
        // Registers the EE implementations behind the CE edition seams (see shared/edition) before the first
        // render, so the hook implementations CE surfaces resolve never change identity afterwards.
        await import('@/ee/shared/edition/registerEditionModules');
    }

    const {helpHub, userGuiding} = applicationInfoStore.getState();

    if (helpHub.enabled && helpHub.commandBar.orgId) {
        const {init} = await import('commandbar');

        init(helpHub.commandBar.orgId);
    }

    if (userGuiding.enabled && userGuiding.containerId) {
        initUserGuiding(userGuiding.containerId);
    }

    if (
        !publicRoutes.find((publicRoute) => window.location.pathname.startsWith(publicRoute)) &&
        !authenticationStore.getState().sessionHasBeenFetched
    ) {
        const result = await authenticationStore.getState().getAccount();

        if (!result && window.location.pathname !== '/login') {
            window.location.replace(buildLoginPath(window.location));
        }
    }

    root.render(
        <StrictMode>
            <I18n>
                <ThemeProvider defaultTheme="system">
                    <QueryClientProvider client={queryClient}>
                        <ConditionalPostHogProvider>
                            <TooltipProvider>
                                <RouterProvider router={router} />
                            </TooltipProvider>
                        </ConditionalPostHogProvider>

                        <ReactQueryDevtools buttonPosition="bottom-right" initialIsOpen={false} />
                    </QueryClientProvider>
                </ThemeProvider>
            </I18n>
        </StrictMode>
    );
}
