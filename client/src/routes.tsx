import App from '@/App';
import ErrorPage from '@/ErrorPage';
import {ProjectApi} from '@/middleware/helios/configuration';
import Connections from '@/pages/automation/connections/Connections';
import OAuthPopup from '@/pages/automation/connections/oauth2/OAuthPopup';
import ProjectInstances from '@/pages/automation/project-instances/ProjectInstances';
import Project from '@/pages/automation/project/Project';
import Projects from '@/pages/automation/projects/Projects';
import WorkflowExecutions from '@/pages/automation/workflow-executions/WorkflowExecutions';
import Account from '@/pages/settings/Account';
import Appearance from '@/pages/settings/Appearance';
import Settings from '@/pages/settings/Settings';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter} from 'react-router-dom';

import {ProjectKeys} from './queries/projects.queries';

const queryClient = new QueryClient();

export const router = createBrowserRouter([
    {
        element: <OAuthPopup />,
        path: '/callback',
    },
    {
        children: [
            {
                element: <Projects />,
                path: '',
            },
            {
                children: [
                    {
                        element: <Project />,
                        loader: async ({params}) =>
                            queryClient.ensureQueryData({
                                queryFn: () =>
                                    new ProjectApi().getProject({
                                        id: parseInt(params.projectId!),
                                    }),
                                queryKey: ProjectKeys.project(
                                    parseInt(params.projectId!)
                                ),
                            }),
                        path: 'projects/:projectId/workflows/:workflowId',
                    },
                    {
                        element: <Projects />,
                        path: 'projects',
                    },
                    {
                        element: <ProjectInstances />,
                        path: 'instances',
                    },
                    {
                        element: <Connections />,
                        path: 'connections',
                    },
                    {
                        element: <WorkflowExecutions />,
                        path: 'executions',
                    },
                ],
                path: 'automation',
            },
            {
                children: [
                    {
                        element: <Account />,
                        path: '',
                    },
                    {
                        element: <Account />,
                        path: 'account',
                    },
                    {
                        element: <Appearance />,
                        path: 'appearance',
                    },
                ],
                element: <Settings />,
                path: 'settings',
            },
        ],
        element: <App />,
        errorElement: <ErrorPage />,
        path: '/',
    },
]);
