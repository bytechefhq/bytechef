import App from '@/App';
import ErrorPage from '@/ErrorPage';
import {ProjectApi} from '@/middleware/automation/configuration';
import Home from '@/pages/Home';
import {Connections as AutomationConnections} from '@/pages/automation/connections/Connections';
import ProjectInstances from '@/pages/automation/project-instances/ProjectInstances';
import Project from '@/pages/automation/project/Project';
import Projects from '@/pages/automation/projects/Projects';
import {WorkflowExecutions as AutomationWorkflowExecutions} from '@/pages/automation/workflow-executions/WorkflowExecutions';
import AppEvents from '@/pages/embedded/app-events/AppEvents';
import {Connections as EmbeddedConnections} from '@/pages/embedded/connections/Connections';
import ConnectedUsers from '@/pages/embedded/integration-instances/ConnectedUsers';
import Integration from '@/pages/embedded/integration/Integration';
import IntegrationPortalConfiguration from '@/pages/embedded/integration/IntegrationPortalConfiguration';
import IntegrationSettings from '@/pages/embedded/integration/IntegrationSettings';
import IntegrationUserMetadata from '@/pages/embedded/integration/IntegrationUserMetadata';
import IntegrationWorkflow from '@/pages/embedded/integration/IntegrationWorkflow';
import Integrations from '@/pages/embedded/integrations/Integrations';
import {WorkflowExecutions as EmbeddedIntegrationWorkflowExecutions} from '@/pages/embedded/workflow-executions/WorkflowExecutions';
import OAuthPopup from '@/pages/platform/connection/components/oauth2/OAuthPopup';
import Account from '@/pages/platform/settings/Account';
import Appearance from '@/pages/platform/settings/Appearance';
import Settings from '@/pages/platform/settings/Settings';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter} from 'react-router-dom';

import {ProjectKeys} from './queries/automation/projects.queries';

const queryClient = new QueryClient();

export const router = createBrowserRouter([
    {
        element: <OAuthPopup />,
        path: '/callback',
    },
    {
        children: [
            {
                element: <Home />,
                path: '',
            },
            {
                children: [
                    {
                        element: <Projects />,
                        path: '',
                    },
                    {
                        children: [],
                        element: <Projects />,
                        path: 'projects',
                    },
                    {
                        element: <Project />,
                        loader: async ({params}) =>
                            queryClient.ensureQueryData({
                                queryFn: () =>
                                    new ProjectApi().getProject({
                                        id: parseInt(params.projectId!),
                                    }),
                                queryKey: ProjectKeys.project(parseInt(params.projectId!)),
                            }),
                        path: 'projects/:projectId/workflows/:workflowId',
                    },
                    {
                        element: <ProjectInstances />,
                        path: 'instances',
                    },
                    {
                        element: <AutomationConnections />,
                        path: 'connections',
                    },
                    {
                        element: <AutomationWorkflowExecutions />,
                        path: 'executions',
                    },
                ],
                path: 'automation',
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
                                element: <IntegrationPortalConfiguration />,
                                path: '',
                            },
                            {
                                element: <IntegrationPortalConfiguration />,
                                path: 'configuration',
                            },
                            {
                                element: <IntegrationWorkflow />,
                                path: 'workflows',
                            },
                            {
                                element: <IntegrationUserMetadata />,
                                path: 'user-metadata',
                            },
                            {
                                element: <IntegrationSettings />,
                                path: 'settings',
                            },
                        ],
                        element: <Integration />,
                        path: 'integrations/:integrationId',
                    },
                    {
                        element: <ConnectedUsers />,
                        path: 'connected-users',
                    },
                    {
                        element: <AppEvents />,
                        path: 'app-events',
                    },
                    {
                        element: <EmbeddedConnections />,
                        path: 'connections',
                    },
                    {
                        element: <EmbeddedIntegrationWorkflowExecutions />,
                        path: 'executions',
                    },
                ],
                path: 'embedded',
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
