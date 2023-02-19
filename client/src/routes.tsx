import {createBrowserRouter} from 'react-router-dom';
import App from './App';
import ErrorPage from './ErrorPage';
import Integration from './pages/integration/Integration';
import Integrations from './pages/integrations/Integrations';
import Connections from './pages/connections/Connections';
import Settings from './pages/settings/Settings';
import Instances from './pages/instances/Instances';
import Executions from './pages/executions/Executions';
import {QueryClient} from '@tanstack/react-query';
import {IntegrationsApi} from './middleware/integration';
import {IntegrationKeys} from './queries/integrations';

const queryClient = new QueryClient();

export const router = createBrowserRouter([
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
                loader: async ({params}) =>
                    queryClient.ensureQueryData(
                        IntegrationKeys.integration(+params.integrationId!),
                        () =>
                            new IntegrationsApi().getIntegration({
                                id: +params.integrationId!,
                            })
                    ),
                path: 'automation/integrations/:integrationId',
                element: <Integration />,
            },
            {
                path: 'automation/integrations',
                element: <Integrations />,
            },
            {
                path: 'automation/instances',
                element: <Instances />,
            },
            {
                path: 'automation/connections',
                element: <Connections />,
            },
            {
                path: 'automation/executions',
                element: <Executions />,
            },
            {
                path: 'settings',
                element: <Settings />,
            },
        ],
    },
]);
