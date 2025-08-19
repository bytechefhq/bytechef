import App from '@/App';
import {IntegrationApi} from '@/ee/shared/middleware/embedded/configuration';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {AccessControl} from '@/shared/auth/AccessControl';
import PrivateRoute from '@/shared/auth/PrivateRoute';
import {AUTHORITIES} from '@/shared/constants';
import EEVersion from '@/shared/edition/EEVersion';
import ErrorPage from '@/shared/error/ErrorPage';
import LazyLoadWrapper from '@/shared/error/LazyLoadWrapper';
import PageNotFound from '@/shared/error/PageNotFound';
import Settings from '@/shared/layout/Settings';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {QueryClient} from '@tanstack/react-query';
import {lazy} from 'react';
import {createBrowserRouter, redirect} from 'react-router-dom';

const ApiClients = lazy(() => import('@/ee/pages/automation/api-platform/api-clients/ApiClients'));
const ApiCollections = lazy(() => import('@/ee/pages/automation/api-platform/api-collections/ApiCollections'));

const AppEvents = lazy(() => import('@/ee/pages/embedded/app-events/AppEvents'));
const AutomationWorkflows = lazy(() => import('@/ee/pages/embedded/automation-workflows/AutomationWorkflows'));
const ConnectedUsers = lazy(() => import('@/ee/pages/embedded/connected-users/ConnectedUsers'));
const EmbeddedConnections = lazy(() =>
    import('@/ee/pages/embedded/connections/Connections').then((module) => ({default: module.Connections}))
);
const IntegrationInstanceConfigurations = lazy(
    () => import('@/ee/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations')
);
const Integration = lazy(() => import('@/ee/pages/embedded/integration/Integration'));
const Integrations = lazy(() => import('@/ee/pages/embedded/integrations/Integrations'));
const EmbeddedIntegrationWorkflowExecutions = lazy(() =>
    import('@/ee/pages/embedded/workflow-executions/WorkflowExecutions').then((module) => ({
        default: module.WorkflowExecutions,
    }))
);

const SigningKeys = lazy(() => import('@/ee/pages/settings/embedded/signing-keys/SigningKeys'));
const ApiConnectors = lazy(() => import('@/ee/pages/settings/platform/api-connectors/ApiConnectors'));
const CustomComponents = lazy(() => import('@/ee/pages/settings/platform/custom-components/CustomComponents'));

const AccountErrorPage = lazy(() => import('@/pages/account/public/AccountErrorPage'));
const Login = lazy(() => import('@/pages/account/public/Login'));
const PasswordResetEmailSent = lazy(() => import('@/pages/account/public/PasswordResetEmailSent'));
const PasswordResetFinish = lazy(() => import('@/pages/account/public/PasswordResetFinish'));
const PasswordResetInit = lazy(() => import('@/pages/account/public/PasswordResetInit'));
const Register = lazy(() => import('@/pages/account/public/Register'));
const RegisterSuccess = lazy(() => import('@/pages/account/public/RegisterSuccess'));
const VerifyEmail = lazy(() => import('@/pages/account/public/VerifyEmail'));

const AccountProfile = lazy(() => import('@/pages/account/settings/AccountProfile'));
const Appearance = lazy(() => import('@/pages/account/settings/Appearance'));
const Sessions = lazy(() => import('@/pages/account/settings/Sessions'));

const AutomationConnections = lazy(() =>
    import('@/pages/automation/connections/Connections').then((module) => ({default: module.Connections}))
);
const McpServers = lazy(() => import('@/pages/automation/mcp-servers/McpServers'));
const ProjectDeployments = lazy(() => import('@/pages/automation/project-deployments/ProjectDeployments'));
const Project = lazy(() => import('@/pages/automation/project/Project'));
const Projects = lazy(() => import('@/pages/automation/projects/Projects'));
const WorkflowChat = lazy(() => import('@/pages/automation/workflow-chat/WorkflowChat'));
const AutomationWorkflowExecutions = lazy(() =>
    import('@/pages/automation/workflow-executions/WorkflowExecutions').then((module) => ({
        default: module.WorkflowExecutions,
    }))
);

const Home = lazy(() => import('@/pages/home/Home'));
const AiProviders = lazy(() => import('@/pages/platform/settings/ai-providers/AiProviders'));
const ApiKeys = lazy(() => import('@/pages/platform/settings/api-keys/ApiKeys'));
const GitConfiguration = lazy(() => import('@/pages/platform/settings/git-configuration/GitConfiguration'));
const Notifications = lazy(() => import('@/pages/platform/settings/notifications/Notifications'));
const Workspaces = lazy(() => import('@/ee/pages/settings/automation/workspaces/Workspaces'));

const getAccountRoutes = (path: string) => ({
    children: [
        {
            element: (
                <PrivateRoute>
                    <LazyLoadWrapper>
                        <AccountProfile />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
            index: true,
        },
        {
            element: (
                <PrivateRoute>
                    <LazyLoadWrapper>
                        <Appearance />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
            path: 'appearance',
        },
        {
            element: (
                <PrivateRoute>
                    <LazyLoadWrapper>
                        <Sessions />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
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

// Current workspace settings routes
const currentWorkspaceSettingsRoutes = {
    children: [
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <GitConfiguration />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'git-configuration',
        },
    ],
    navItems: [
        {
            title: 'Current Workspace',
        },
        {
            href: 'git-configuration',
            title: 'Git Configuration',
        },
    ],
};

// Platform settings routes
const platformSettingsRoutes = {
    children: [
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <AiProviders />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'ai-providers',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <CustomComponents />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'custom-components',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <ApiConnectors />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'api-connectors',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <LazyLoadWrapper>
                        <ApiKeys />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
            path: 'api-keys',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <LazyLoadWrapper>
                        <Notifications />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
            path: 'notifications',
        },
    ],
    navItems: [
        {
            title: 'Organization',
        },
        {
            href: 'ai-providers',
            title: 'AI Providers',
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
        {
            href: 'notifications',
            title: 'Notifications',
        },
    ],
};

export const getRouter = (queryClient: QueryClient) =>
    createBrowserRouter([
        // {
        //     element: <OAuthPopup />,
        //     path: '/oauth',
        // },
        {
            element: <WorkflowChat />,
            path: 'chat/:workflowExecutionId',
        },
        {
            element: <WorkflowChat />,
            path: 'chat/:environment/:workflowExecutionId',
        },
        {
            children: [
                {
                    element: (
                        <LazyLoadWrapper>
                            <Login />
                        </LazyLoadWrapper>
                    ),
                    path: '/login',
                },
                {
                    element: (
                        <LazyLoadWrapper>
                            <Register />
                        </LazyLoadWrapper>
                    ),
                    path: '/register',
                },
                {
                    element: (
                        <LazyLoadWrapper>
                            <PasswordResetInit />
                        </LazyLoadWrapper>
                    ),
                    path: '/password-reset/init',
                },
                {
                    element: (
                        <AccessControl requiresFlow requiresKey>
                            <LazyLoadWrapper>
                                <RegisterSuccess />
                            </LazyLoadWrapper>
                        </AccessControl>
                    ),
                    path: '/activate',
                },
                {
                    element: (
                        <AccessControl requiresKey>
                            <LazyLoadWrapper>
                                <PasswordResetFinish />
                            </LazyLoadWrapper>
                        </AccessControl>
                    ),
                    path: '/password-reset/finish',
                },
                {
                    element: (
                        <AccessControl requiresFlow>
                            <LazyLoadWrapper>
                                <PasswordResetEmailSent />
                            </LazyLoadWrapper>
                        </AccessControl>
                    ),
                    path: '/password-reset/email',
                },
                {
                    element: (
                        <AccessControl requiresFlow>
                            <LazyLoadWrapper>
                                <VerifyEmail />
                            </LazyLoadWrapper>
                        </AccessControl>
                    ),
                    path: '/verify-email',
                },
                {
                    element: (
                        <AccessControl requiresFlow>
                            <LazyLoadWrapper>
                                <AccountErrorPage />
                            </LazyLoadWrapper>
                        </AccessControl>
                    ),
                    path: '/account-error',
                },
                {
                    children: [
                        {
                            element: (
                                <LazyLoadWrapper>
                                    <Home />
                                </LazyLoadWrapper>
                            ),
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
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <Projects />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'projects',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper>
                                                <Project />
                                            </LazyLoadWrapper>
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
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <ProjectDeployments />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'deployments',
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
                                                        <LazyLoadWrapper hasLeftSidebar>
                                                            <ApiCollections />
                                                        </LazyLoadWrapper>
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'api-collections',
                                        },
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                                    <EEVersion>
                                                        <LazyLoadWrapper hasLeftSidebar>
                                                            <ApiClients />
                                                        </LazyLoadWrapper>
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
                                            <LazyLoadWrapper>
                                                <McpServers />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'mcp-servers',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AutomationWorkflowExecutions />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'executions',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AutomationConnections />
                                            </LazyLoadWrapper>
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
                                                        <LazyLoadWrapper hasLeftSidebar>
                                                            <Workspaces />
                                                        </LazyLoadWrapper>
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'workspaces',
                                        },
                                        ...currentWorkspaceSettingsRoutes.children,
                                        ...platformSettingsRoutes.children,
                                    ],
                                    element: (
                                        <Settings
                                            sidebarNavItems={[
                                                {
                                                    href: '/automation/settings/workspaces',
                                                    title: 'Workspaces',
                                                },
                                                ...currentWorkspaceSettingsRoutes.navItems,
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
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <Integrations />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'integrations',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper>
                                                <EEVersion>
                                                    <Integration />
                                                </EEVersion>
                                            </LazyLoadWrapper>
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
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <IntegrationInstanceConfigurations />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'configurations',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <AutomationWorkflows />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'automation-workflows',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <ConnectedUsers />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'connected-users',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <AppEvents />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'app-events',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <EmbeddedIntegrationWorkflowExecutions />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'executions',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <EmbeddedConnections />
                                                </LazyLoadWrapper>
                                            </EEVersion>
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
                                                    <EEVersion>
                                                        <LazyLoadWrapper>
                                                            <SigningKeys />
                                                        </LazyLoadWrapper>
                                                    </EEVersion>
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
