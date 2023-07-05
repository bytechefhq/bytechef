import IntegrationConfiguration from '@/pages/embedded/integration/IntegrationConfiguration';
import IntegrationSettings from '@/pages/embedded/integration/IntegrationSettings';
import IntegrationWorkflowEditor from '@/pages/embedded/integration/IntegrationWorkflowEditor';
import Account from '@/pages/settings/Account';
import Appearance from '@/pages/settings/Appearance';
import Settings from '@/pages/settings/Settings';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter} from 'react-router-dom';

import App from './App';
import ErrorPage from './ErrorPage';
import {ProjectsApi} from './middleware/automation/configuration';
import Connections from './pages/automation/connections/Connections';
import OAuthPopup from './pages/automation/connections/oauth2/OAuthPopup';
import ProjectInstances from './pages/automation/project-instances/ProjectInstances';
import Project from './pages/automation/project/Project';
import Projects from './pages/automation/projects/Projects';
import WorkflowExecutions from './pages/automation/workflow-executions/WorkflowExecutions';
import Integrations from './pages/embedded/integrations/Integrations';
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
            {
                children: [
                    {
                        element: <Integrations />,
                        path: '',
                    },
                    {
                        element: <Integrations />,
                        path: 'integrations',
                    },
                    {
                        children: [
                            {
                                element: <IntegrationConfiguration />,
                                path: 'configuration',
                            },
                            {
                                element: <IntegrationWorkflowEditor />,
                                path: 'workflows',
                            },
                            {
                                element: <IntegrationSettings />,
                                path: 'settings',
                            },
                        ],
                        path: 'integrations/:integrationId',
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
