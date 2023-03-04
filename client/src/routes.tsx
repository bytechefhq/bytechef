import {createBrowserRouter} from 'react-router-dom';
import App from './App';
import ErrorPage from './ErrorPage';
import Integration from './pages/integration/Integration';
import Integrations from './pages/embedded/integrations/Integrations';
import Connections from './pages/connections/Connections';
import Settings from './pages/settings/Settings';
import Instances from './pages/instances/Instances';
import Executions from './pages/executions/Executions';
import {QueryClient} from '@tanstack/react-query';
import {IntegrationsApi} from './middleware/integration';
import {IntegrationKeys} from './queries/integrations';
import OAuthPopup from './pages/connections/oauth2/OAuthPopup';

const queryClient = new QueryClient();

export const router = createBrowserRouter([
    {
        element: <OAuthPopup />,
        path: '/callback',
    },
    {
        path: '/',
        element: <App />,
        errorElement: <ErrorPage />,
        children: [
            {
                path: '',
                element: <Integrations />,
            },
            {
                path: 'automation',
                children: [
                    {
                        loader: async ({params}) =>
                            queryClient.ensureQueryData(
                                IntegrationKeys.integration(
                                    +params.integrationId!
                                ),
                                () =>
                                    new IntegrationsApi().getIntegration({
                                        id: +params.integrationId!,
                                    })
                            ),
                        path: 'integrations/:integrationId',
                        element: <Integration />,
                    },
                    {
                        path: 'integrations',
                        element: <Integrations />,
                    },
                    {
                        path: 'instances',
                        element: <Instances />,
                    },
                    {
                        path: 'connections',
                        element: <Connections />,
                    },
                    {
                        path: 'executions',
                        element: <Executions />,
                    },
                    {
                        path: 'settings',
                        element: <Settings />,
                    },
                ],
            },
            {
                path: 'embedded',
                children: [
                    {
                        path: 'integrations',
                        element: <Integrations />,
                    },
                ],
            },
        ],
    },
]);
