import App from '@/App';
import ApiClients from '@/ee/pages/automation/api-platform/api-clients/ApiClients';
import ApiCollections from '@/ee/pages/automation/api-platform/api-collections/ApiCollections';
import ApiConnectors from '@/ee/pages/settings/platform/api-connectors/ApiConnectors';
import Activate from '@/pages/account/public/Activate';
import Login from '@/pages/account/public/Login';
import PasswordResetFinish from '@/pages/account/public/PasswordResetFinish';
import PasswordResetInit from '@/pages/account/public/PasswordResetInit';
import Register from '@/pages/account/public/Register';
import VerifyEmail from '@/pages/account/public/VerifyEmail';
import AccountProfile from '@/pages/account/settings/AccountProfile';
import Appearance from '@/pages/account/settings/Appearance';
import Sessions from '@/pages/account/settings/Sessions';
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
import Integrations from '@/pages/embedded/integrations/Integrations';
import {WorkflowExecutions as EmbeddedIntegrationWorkflowExecutions} from '@/pages/embedded/workflow-executions/WorkflowExecutions';
import Home from '@/pages/home/Home';
import OAuthPopup from '@/pages/platform/connection/components/oauth2/OAuthPopup';
import ApiKeys from '@/pages/platform/settings/api-keys/ApiKeys';
import Workspaces from '@/pages/settings/automation/workspaces/Workspaces';
import SigningKeys from '@/pages/settings/embedded/signing-keys/SigningKeys';
import CustomComponents from '@/pages/settings/platform/custom-components/CustomComponents';
import PrivateRoute from '@/shared/auth/PrivateRoute';
import {AUTHORITIES} from '@/shared/constants';
import EEVersion from '@/shared/edition/EEVersion';
import ErrorPage from '@/shared/error/ErrorPage';
import PageNotFound from '@/shared/error/PageNotFound';
import Settings from '@/shared/layout/Settings';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {IntegrationApi} from '@/shared/middleware/embedded/configuration';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {QueryClient} from '@tanstack/react-query';
import {createBrowserRouter, redirect} from 'react-router-dom';

const getAccountRoutes = (path: string) => ({
    children: [
        {
            element: <AccountProfile />,
            index: true,
        },
        {
            element: <Appearance />,
            path: 'appearance',
        },
        {
            element: <Sessions />,
            path: 'sessions',
        },
    ],
    element: (
        <Settings
            sidebarNavItems={[
                {
                    href: `${path}/account`,
                    title: 'Profile',
                },
                {
                    href: `${path}/account/appearance`,
                    title: 'Appearance',
                },
                {
                    href: `${path}/account/sessions`,
                    title: 'Active sessions',
                },
            ]}
            title="Your Account"
        />
    ),
    path: 'account',
});

const platformSettingsRoutes = {
    children: [
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <CustomComponents />
                </PrivateRoute>
            ),
            path: 'custom-components',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <ApiConnectors />
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'api-connectors',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <ApiKeys />
                </PrivateRoute>
            ),
            path: 'api-keys',
        },
    ],
    navItems: [
        {
            title: 'Organization',
        },
        {
            href: 'custom-components',
            title: 'Custom Components',
        },
        {
            href: `api-connectors`,
            title: 'API Connectors',
        },
        {
            href: 'api-keys',
            title: 'API Keys',
        },
    ],
};

export const getRouter = (queryClient: QueryClient) =>
    createBrowserRouter([
        {
            element: <OAuthPopup />,
            path: '/callback',
        },
        {
            children: [
                {
                    element: <Activate />,
                    path: 'activate',
                },
                {
                    element: <Login />,
                    path: '/login',
                },
                {
                    element: <Register />,
                    path: '/register',
                },
                {
                    children: [
                        {
                            element: <PasswordResetInit />,
                            path: 'init',
                        },
                        {
                            element: <PasswordResetFinish />,
                            path: 'finish',
                        },
                    ],
                    path: 'password-reset',
                },
                {
                    element: <VerifyEmail />,
                    path: '/verify-email',
                },
                {
                    children: [
                        {
                            element: <Home />,
                            index: true,
                        },
                        {
                            children: [
                                {
                                    index: true,
                                    loader: async () => {
                                        return redirect('projects');
                                    },
                                },
                                getAccountRoutes('/automation'),
                                {
                                    children: [],
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <Projects />
                                        </PrivateRoute>
                                    ),
                                    path: 'projects',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <Project />
                                        </PrivateRoute>
                                    ),
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
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <ProjectInstances />
                                        </PrivateRoute>
                                    ),
                                    path: 'instances',
                                },
                                {
                                    children: [
                                        {
                                            index: true,
                                            loader: async () => {
                                                return redirect('api-collections');
                                            },
                                        },
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                                    <EEVersion>
                                                        <ApiCollections />
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'api-collections',
                                        },
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                                    <EEVersion>
                                                        <ApiClients />
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'api-clients',
                                        },
                                    ],
                                    path: 'api-platform',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <AutomationWorkflowExecutions />
                                        </PrivateRoute>
                                    ),
                                    path: 'executions',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <AutomationConnections />
                                        </PrivateRoute>
                                    ),
                                    path: 'connections',
                                },
                                {
                                    children: [
                                        {
                                            index: true,
                                            loader: async () => {
                                                return redirect('workspaces');
                                            },
                                        },
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                                                    <EEVersion>
                                                        <Workspaces />
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'workspaces',
                                        },
                                        ...platformSettingsRoutes.children,
                                    ],
                                    element: (
                                        <Settings
                                            sidebarNavItems={[
                                                {
                                                    href: '/automation/settings/workspaces',
                                                    title: 'Workspaces',
                                                },
                                                ...platformSettingsRoutes.navItems,
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
                                getAccountRoutes('/embedded'),
                                {
                                    element: <Integrations />,
                                    path: 'integrations',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <Integration />
                                        </PrivateRoute>
                                    ),
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
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <IntegrationInstanceConfigurations />
                                        </PrivateRoute>
                                    ),
                                    path: 'configurations',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <ConnectedUsers />
                                        </PrivateRoute>
                                    ),
                                    path: 'connected-users',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <AppEvents />
                                        </PrivateRoute>
                                    ),
                                    path: 'app-events',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EmbeddedIntegrationWorkflowExecutions />
                                        </PrivateRoute>
                                    ),
                                    path: 'executions',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EmbeddedConnections />
                                        </PrivateRoute>
                                    ),
                                    path: 'connections',
                                },
                                {
                                    children: [
                                        {
                                            index: true,
                                            loader: async () => {
                                                return redirect('signing-keys');
                                            },
                                        },
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                                                    <SigningKeys />
                                                </PrivateRoute>
                                            ),
                                            path: 'signing-keys',
                                        },
                                        ...platformSettingsRoutes.children,
                                    ],
                                    element: (
                                        <Settings
                                            sidebarNavItems={[
                                                {
                                                    href: '/embedded/settings/signing-keys',
                                                    title: 'Signing Keys',
                                                },
                                                ...platformSettingsRoutes.navItems,
                                            ]}
                                        />
                                    ),
                                    path: 'settings',
                                },
                            ],
                            errorElement: <ErrorPage />,
                            path: 'embedded',
                        },
                    ],
                    path: '/',
                },
            ],
            element: <App />,
            errorElement: <ErrorPage />,
            path: '/',
        },
        {
            element: <PageNotFound />,
            path: '*',
        },
    ]);
