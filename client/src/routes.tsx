import App from '@/App';
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
import Account from '@/pages/settings/Account';
import Appearance from '@/pages/settings/Appearance';
import Settings from '@/pages/settings/Settings';
import Workspaces from '@/pages/settings/automation/workspaces/Workspaces';
import ApiKeys from '@/pages/settings/embedded/api-keys/ApiKeys';
import SigningKeys from '@/pages/settings/embedded/signing-keys/SigningKeys';
import ErrorPage from '@/shared/error/ErrorPage';
import PageNotFound from '@/shared/error/PageNotFound';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {IntegrationApi} from '@/shared/middleware/embedded/configuration';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter, redirect} from 'react-router-dom';

export const getRouter = (queryClient: QueryClient) =>
    createBrowserRouter([
        {
            element: <OAuthPopup />,
            path: '/callback',
        },
        {
            children: [
                {
                },
                {
                    children: [
                        {
                            index: true,
                            loader: async () => {
                                return redirect('projects');
                            },
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
                        {
                            children: [
                                {
                                    index: true,
                                    loader: async () => {
                                        return redirect('account');
                                    },
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
                                    element: <Workspaces />,
                                    path: 'workspaces',
                                },
                            ],
                            element: (
                                <Settings
                                    sidebarNavItems={[
                                        {
                                            href: '/automation/settings/account',
                                            title: 'Account',
                                        },
                                        {
                                            href: '/automation/settings/appearance',
                                            title: 'Appearance',
                                        },
                                        {
                                            href: '/automation/settings/workspaces',
                                            title: 'Workspaces',
                                        },
                                    ]}
                                />
                            ),
                            path: 'settings',
                        },
                    ],
                    errorElement: <ErrorPage />,
                    path: 'automation',
                },
                {
                    children: [
                        {
                            index: true,
                            loader: async () => {
                                return redirect('integrations');
                            },
                        },
                        {
                            children: [
                                {
                                    element: <EmbeddedIPaaSIntegrations />,
                                    index: true,
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
                        {
                            children: [
                                {
                                    index: true,
                                    loader: async () => {
                                        return redirect('api-keys');
                                    },
                                },
                                {
                                    element: <ApiKeys />,
                                    path: 'api-keys',
                                },
                                {
                                    element: <SigningKeys />,
                                    path: 'signing-keys',
                                },
                            ],
                            element: (
                                <Settings
                                    sidebarNavItems={[
                                        {
                                            href: '/embedded/settings/api-keys',
                                            title: 'API Keys',
                                        },
                                        {
                                            href: '/embedded/settings/signing-keys',
                                            title: 'Signing Keys',
                                        },
                                    ]}
                                />
                            ),
                            path: 'settings',
                        },
                    ],
                    errorElement: <ErrorPage />,
                    path: 'embedded',
                },
                {
                    element: <PageNotFound />,
                    path: '*',
                },
            ],
            element: <App />,
            errorElement: <ErrorPage />,
            path: '/',
        },
        {
            element: <Home />,
            index: true,
        },
    ]);
