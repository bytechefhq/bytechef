import {createBrowserRouter} from 'react-router-dom';
import App from './App';
import ErrorPage from './ErrorPage';
import Project from './pages/automation/project/Project';
import Integrations from './pages/embedded/integrations/Integrations';
import Connections from './pages/connections/Connections';
import Settings from './pages/settings/Settings';
import Instances from './pages/instances/Instances';
import Executions from './pages/executions/Executions';
import {QueryClient} from '@tanstack/react-query';
import {ProjectsApi} from './middleware/project';
import {ProjectKeys} from './queries/projects';
import OAuthPopup from './pages/connections/oauth2/OAuthPopup';
import Projects from './pages/automation/projects/Projects';

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
                element: <Projects />,
            },
            {
                path: 'automation',
                children: [
                    {
                        loader: async ({params}) =>
                            queryClient.ensureQueryData(
                                ProjectKeys.project(+params.projectId!),
                                () =>
                                    new ProjectsApi().getProject({
                                        id: +params.projectId!,
                                    })
                            ),
                        path: 'integrations/:integrationId',
                        element: <Project />,
                    },
                    {
                        path: 'projects',
                        element: <Projects />,
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
