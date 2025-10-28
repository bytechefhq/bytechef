import App from '@/App';
import {IntegrationApi} from '@/ee/shared/middleware/embedded/configuration';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import AccountErrorPage from '@/pages/account/public/AccountErrorPage';
import Login from '@/pages/account/public/Login';
import PasswordResetEmailSent from '@/pages/account/public/PasswordResetEmailSent';
import PasswordResetFinish from '@/pages/account/public/PasswordResetFinish';
import PasswordResetInit from '@/pages/account/public/PasswordResetInit';
import Register from '@/pages/account/public/Register';
import RegisterSuccess from '@/pages/account/public/RegisterSuccess';
import VerifyEmail from '@/pages/account/public/VerifyEmail';
import {Connections} from '@/pages/automation/connections/Connections';
import ProjectDeployments from '@/pages/automation/project-deployments/ProjectDeployments';
import {AccessControl} from '@/shared/auth/AccessControl';
import PrivateRoute from '@/shared/auth/PrivateRoute';
import {AUTHORITIES} from '@/shared/constants';
import EEVersion from '@/shared/edition/EEVersion';
import ErrorPage from '@/shared/error/ErrorPage';
import LazyLoadWrapper from '@/shared/error/LazyLoadWrapper';
import PageNotFound from '@/shared/error/PageNotFound';
import Settings from '@/shared/layout/Settings';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {EnvironmentApi} from '@/shared/middleware/platform/configuration';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {EnvironmentKeys} from '@/shared/queries/platform/environments.queries';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient} from '@tanstack/react-query';
import {lazy} from 'react';
import {createBrowserRouter, redirect} from 'react-router-dom';

const AccountProfile = lazy(() => import('@/pages/account/settings/AccountProfile'));
const Appearance = lazy(() => import('@/pages/account/settings/Appearance'));
const ApiKeys = lazy(() => import('@/pages/settings/platform/api-keys/ApiKeys'));
const AutomationWorkflowExecutions = lazy(() =>
    import('@/pages/automation/workflow-executions/WorkflowExecutions').then((module) => ({
        default: module.WorkflowExecutions,
    }))
);
const Home = lazy(() => import('@/pages/home/Home'));
const McpServers = lazy(() => import('@/pages/automation/mcp-servers/McpServers'));
const Notifications = lazy(() => import('@/pages/settings/platform/notifications/Notifications'));
const Project = lazy(() => import('@/pages/automation/project/Project'));
const ProjectTemplate = lazy(() => import('@/pages/automation/template/project-template/ProjectTemplate'));
const ProjectTemplates = lazy(() => import('@/pages/automation/templates/project-templates/ProjectTemplates'));
const Projects = lazy(() => import('@/pages/automation/projects/Projects'));

const Sessions = lazy(() => import('@/pages/account/settings/Sessions'));
const WorkflowChat = lazy(() => import('@/pages/automation/workflow-chat/WorkflowChat'));
const WorkflowTemplate = lazy(() => import('@/pages/automation/template/workflow-template/WorkflowTemplate'));
const WorkflowTemplates = lazy(() => import('@/pages/automation/templates/workflow-templates/WorkflowTemplates'));

const AiProviders = lazy(() => import('@/ee/pages/settings/platform/ai-providers/AiProviders'));
const ApiClients = lazy(() => import('@/ee/pages/automation/api-platform/api-clients/ApiClients'));
const ApiCollections = lazy(() => import('@/ee/pages/automation/api-platform/api-collections/ApiCollections'));
const ApiConnectors = lazy(() => import('@/ee/pages/settings/platform/api-connectors/ApiConnectors'));
const AppEvents = lazy(() => import('@/ee/pages/embedded/app-events/AppEvents'));
const AutomationWorkflows = lazy(() => import('@/ee/pages/embedded/automation-workflows/AutomationWorkflows'));
const ConnectedUsers = lazy(() => import('@/ee/pages/embedded/connected-users/ConnectedUsers'));
const CustomComponents = lazy(() => import('@/ee/pages/settings/platform/custom-components/CustomComponents'));
const EmbeddedConnections = lazy(() =>
    import('@/ee/pages/embedded/connections/Connections').then((module) => ({default: module.Connections}))
);
const EmbeddedIntegrationWorkflowExecutions = lazy(() =>
    import('@/ee/pages/embedded/workflow-executions/WorkflowExecutions').then((module) => ({
        default: module.WorkflowExecutions,
    }))
);
const GitConfiguration = lazy(() => import('@/ee/pages/settings/platform/git-configuration/GitConfiguration'));
const IntegrationInstanceConfigurations = lazy(
    () => import('@/ee/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations')
);
const Integration = lazy(() => import('@/ee/pages/embedded/integration/Integration'));
const Integrations = lazy(() => import('@/ee/pages/embedded/integrations/Integrations'));
const SigningKeys = lazy(() => import('@/ee/pages/settings/embedded/signing-keys/SigningKeys'));
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

export const loadEnvironments = async (queryClient: QueryClient) => {
    if (authenticationStore.getState().authenticated) {
        const environments = await queryClient.fetchQuery({
            queryFn: () => new EnvironmentApi().getEnvironments(),
            queryKey: EnvironmentKeys,
        });

        environmentStore.getState().setEnvironments(environments);
    }
};

export const getRouter = (queryClient: QueryClient) =>
    createBrowserRouter([
        // {
        //     element: <OAuthPopup />,
        //     path: '/oauth',
        // },
        {
            element: <Login />,
            path: '/login',
        },
        {
            element: <Register />,
            path: '/register',
        },
        {
            element: <PasswordResetInit />,
            path: '/password-reset/init',
        },
        {
            element: (
                <AccessControl requiresFlow requiresKey>
                    <RegisterSuccess />
                </AccessControl>
            ),
            path: '/activate',
        },
        {
            element: (
                <AccessControl requiresKey>
                    <PasswordResetFinish />
                </AccessControl>
            ),
            path: '/password-reset/finish',
        },
        {
            element: (
                <AccessControl requiresFlow>
                    <PasswordResetEmailSent />
                </AccessControl>
            ),
            path: '/password-reset/email',
        },
        {
            element: (
                <AccessControl requiresFlow>
                    <VerifyEmail />
                </AccessControl>
            ),
            path: '/verify-email',
        },
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
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <LazyLoadWrapper>
                                <ProjectTemplate sharedProject />
                            </LazyLoadWrapper>
                        </PrivateRoute>
                    ),
                    path: 'shared/projects/:id',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <LazyLoadWrapper>
                                <WorkflowTemplate sharedWorkflow />
                            </LazyLoadWrapper>
                        </PrivateRoute>
                    ),
                    path: 'shared/workflows/:id',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <LazyLoadWrapper>
                                <ProjectTemplate />
                            </LazyLoadWrapper>
                        </PrivateRoute>
                    ),
                    path: 'template/projects/:id',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <LazyLoadWrapper>
                                <WorkflowTemplate />
                            </LazyLoadWrapper>
                        </PrivateRoute>
                    ),
                    path: 'template/workflows/:id',
                },
            ],
            path: 'import',
        },
        {
            children: [
                {
                    element: (
                        <AccessControl requiresFlow>
                            <AccountErrorPage />
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
                                            <LazyLoadWrapper>
                                                <ProjectTemplates />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'projects/templates',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper>
                                                <ProjectTemplate fromInternalFlow sharedProject={false} />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'projects/templates/:id',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper>
                                                <WorkflowTemplates />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'projects/:projectId/templates',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper>
                                                <WorkflowTemplate fromInternalFlow sharedWorkflow={false} />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'projects/:projectId/templates/:id',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <ProjectDeployments />
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
                                            <Connections />
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
            loader: async () => {
                await loadEnvironments(queryClient);
            },
            path: '/',
        },
        {
            element: <PageNotFound />,
            path: '*',
        },
    ]);
