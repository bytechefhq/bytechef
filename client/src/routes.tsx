import App from '@/App';
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
import AdminApiKeys from '@/pages/platform/settings/admin-api-keys/AdminApiKeys';
import {ApiKeys as AutomationApiKeys} from '@/pages/settings/automation/api-keys/ApiKeys';
import Workspaces from '@/pages/settings/automation/workspaces/Workspaces';
import {ApiKeys as EmbeddedApiKeys} from '@/pages/settings/embedded/api-keys/ApiKeys';
import SigningKeys from '@/pages/settings/embedded/signing-keys/SigningKeys';
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
            element: <AdminApiKeys />,
            path: 'admin-api-keys',
        },
    ],
    navItems: [
        {
            title: 'Organization',
        },
        {
            href: `api-connectors`,
            title: 'API Connectors',
        },
        {
            href: `admin-api-keys`,
            title: 'Admin API Keys',
        },
    ],
};

export const getRouter = (queryClient: QueryClient) =>
    createBrowserRouter([
        {
            element: <Activate />,
            path: 'activate',
        },
        {
            element: <OAuthPopup />,
            path: '/callback',
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
                            element: (
                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                    <AutomationConnections />
                                </PrivateRoute>
                            ),
                            path: 'connections',
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
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                                            <AutomationApiKeys />
                                        </PrivateRoute>
                                    ),
                                    path: 'api-keys',
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
                                        {
                                            href: '/automation/settings/api-keys',
                                            title: 'API Keys',
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
                                    <EmbeddedConnections />
                                </PrivateRoute>
                            ),
                            path: 'connections',
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
                            children: [
                                {
                                    index: true,
                                    loader: async () => {
                                        return redirect('api-keys');
                                    },
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                                            <EmbeddedApiKeys />
                                        </PrivateRoute>
                                    ),
                                    path: 'api-keys',
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
                                            href: '/embedded/settings/api-keys',
                                            title: 'API Keys',
                                        },
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
                {
                    element: <Home />,
                    index: true,
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
