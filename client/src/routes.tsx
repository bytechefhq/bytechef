import App from '@/App';
import ErrorPage from '@/ErrorPage';
import {ProjectApi} from '@/middleware/automation/configuration';
import {IntegrationApi} from '@/middleware/embedded/configuration';
import Home from '@/pages/Home';
import {Connections as AutomationConnections} from '@/pages/automation/connections/Connections';
import ProjectInstances from '@/pages/automation/project-instances/ProjectInstances';
import Project from '@/pages/automation/project/Project';
import Projects from '@/pages/automation/projects/Projects';
import {WorkflowExecutions as AutomationWorkflowExecutions} from '@/pages/automation/workflow-executions/WorkflowExecutions';
import AppEvents from '@/pages/embedded/app-events/AppEvents';
import ConnectedUsers from '@/pages/embedded/connected-users/ConnectedUsers';
import {Connections as EmbeddedConnections} from '@/pages/embedded/connections/Connections';
import IntegrationInstanceConfigurations from '@/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations';
import Integration from '@/pages/embedded/integration/Integration';
import EmbeddedIPaaSIntegrations from '@/pages/embedded/integrations/EmbeddedIPaaSIntegrations';
import Integrations from '@/pages/embedded/integrations/Integrations';
import {WorkflowExecutions as EmbeddedIntegrationWorkflowExecutions} from '@/pages/embedded/workflow-executions/WorkflowExecutions';
import OAuthPopup from '@/pages/platform/connection/components/oauth2/OAuthPopup';
import Account from '@/pages/platform/settings/Account';
import Appearance from '@/pages/platform/settings/Appearance';
import Settings from '@/pages/platform/settings/Settings';
import ApiKeys from '@/pages/platform/settings/embedded/api-keys/ApiKeys';
import SigningKeys from '@/pages/platform/settings/embedded/signing-keys/SigningKeys';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter, redirect} from 'react-router-dom';

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
                loader: async () => {
                    return redirect('automation');
                },
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
                        path: 'projects/:projectId/project-workflows/:projectWorkflowId',
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
                        loader: async () => {
                            return redirect('integrations');
                        },
                        path: '',
                    },
                    {
                        children: [
                            {
                                element: <EmbeddedIPaaSIntegrations />,
                                path: '',
                            },
                        ],
                        element: <Integrations />,
                        path: 'integrations',
                    },
                    {
                        element: <Integration />,
                        loader: async ({params}) =>
                            queryClient.ensureQueryData({
                                queryFn: () =>
                                    new IntegrationApi().getIntegration({
                                        id: parseInt(params.integrationId!),
                                    }),
                                queryKey: IntegrationKeys.integration(parseInt(params.integrationId!)),
                            }),
                        path: 'integrations/:integrationId/integration-workflows/:integrationWorkflowId',
                    },
                    {
                        element: <IntegrationInstanceConfigurations />,
                        path: 'configurations',
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
                    {
                        element: <ApiKeys />,
                        path: 'e/api-keys',
                    },
                    {
                        element: <SigningKeys />,
                        path: 'e/signing-keys',
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
