import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter} from 'react-router-dom';

import App from './App';
import ErrorPage from './ErrorPage';
import {ProjectsApi} from './middleware/automation/project';
import Connections from './pages/automation/connections/Connections';
import OAuthPopup from './pages/automation/connections/oauth2/OAuthPopup';
import ProjectInstances from './pages/automation/project-instances/ProjectInstances';
import Project from './pages/automation/project/Project';
import Projects from './pages/automation/projects/Projects';
import WorkflowExecutions from './pages/automation/workflow-executions/WorkflowExecutions';
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
                    },
                    {
                        element: <Projects />,
                        path: 'projects',
                    },
                    {
                        element: <ProjectInstances />,
                        path: 'project-instances',
                    },
                    {
                        element: <Connections />,
                        path: 'connections',
                    },
                    {
                        element: <WorkflowExecutions />,
                        path: 'workflow-executions',
                    },
                ],
                path: 'automation',
            },
            {
                element: <Settings />,
                path: 'settings',
            },
            {
                children: [
                    {
                        element: <Integrations />,
                        path: 'integrations',
                    },
                ],
                path: 'embedded',
            },
        ],
        element: <App />,
        errorElement: <ErrorPage />,
        path: '/',
    },
]);
