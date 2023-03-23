import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter} from 'react-router-dom';

import App from './App';
import ErrorPage from './ErrorPage';
import {ProjectsApi} from './middleware/project';
import Connections from './pages/automation/connections/Connections';
import OAuthPopup from './pages/automation/connections/oauth2/OAuthPopup';
import Executions from './pages/automation/executions/Executions';
import Instances from './pages/automation/instances/Instances';
import Project from './pages/automation/project/Project';
import Projects from './pages/automation/projects/Projects';
import Integrations from './pages/embedded/integrations/Integrations';
import Settings from './pages/settings/Settings';
import {ProjectKeys} from './queries/projects.queries';

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
                                ProjectKeys.project(
                                    parseInt(params.projectId!)
                                ),
                                () =>
                                    new ProjectsApi().getProject({
                                        id: parseInt(params.projectId!),
                                    })
                            ),
                        path: 'projects/:projectId/workflow/:workflowId',
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
                ],
            },
            {
                path: 'settings',
                element: <Settings />,
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
