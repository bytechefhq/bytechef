import AccountErrorPage from '@/pages/account/public/AccountErrorPage';
import Login from '@/pages/account/public/Login';
import OAuth2Consent from '@/pages/account/public/OAuth2Consent';
import OAuth2Redirect from '@/pages/account/public/OAuth2Redirect';
import PasswordResetEmailSent from '@/pages/account/public/PasswordResetEmailSent';
import PasswordResetFinish from '@/pages/account/public/PasswordResetFinish';
import PasswordResetInit from '@/pages/account/public/PasswordResetInit';
import Register from '@/pages/account/public/Register';
import RegisterSuccess from '@/pages/account/public/RegisterSuccess';
import VerifyEmail from '@/pages/account/public/VerifyEmail';
import ResumeForm from '@/pages/automation/resume-form/ResumeForm';
import TriggerForm from '@/pages/automation/trigger-form/TriggerForm';
import {AccessControl} from '@/shared/auth/AccessControl';
import PrivateRoute from '@/shared/auth/PrivateRoute';
import {AUTHORITIES, DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
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
import {Suspense, lazy} from 'react';
import {Navigate, createBrowserRouter, redirect, useLocation} from 'react-router-dom';

const App = lazy(() => import('@/App'));
const AccountProfile = lazy(() => import('@/pages/account/settings/AccountProfile'));
const AiGateway = lazy(() => import('@/pages/automation/ai/gateway/AiGateway'));
const AiSkills = lazy(() => import('@/pages/automation/ai/skills/AiSkills'));
const Appearance = lazy(() => import('@/pages/account/settings/Appearance'));
const AutomationConnections = lazy(() =>
    import('@/pages/automation/connections/Connections').then((module) => ({default: module.Connections}))
);
const AutomationToolInvocationsPage = lazy(() =>
    import('@/pages/automation/tool-invocations/AutomationToolInvocations').then((module) => ({
        default: module.AutomationToolInvocations,
    }))
);
const ToolInvocationsPage = lazy(() =>
    import('@/pages/automation/tool-invocations/ToolInvocations').then((module) => ({
        default: module.ToolInvocations,
    }))
);
const AutomationWorkflowExecutions = lazy(() =>
    import('@/pages/automation/workflow-executions/WorkflowExecutions').then((module) => ({
        default: module.WorkflowExecutions,
    }))
);
const DataTables = lazy(() => import('@/pages/automation/datatables/DataTables'));
const DataTable = lazy(() => import('@/pages/automation/datatable/DataTable'));
const Home = lazy(() => import('@/pages/home/Home'));
const KnowledgeBase = lazy(() => import('@/pages/automation/knowledge-base/KnowledgeBase'));
const KnowledgeBases = lazy(() => import('@/pages/automation/knowledge-bases/KnowledgeBases'));
const McpServer = lazy(() => import('@/pages/settings/platform/mcp-server/McpServer'));
const A2aServers = lazy(() => import('@/pages/automation/a2a-servers/A2aServers'));
const Agents = lazy(() => import('@/pages/automation/agents/Agents'));
const AgentDetail = lazy(() => import('@/pages/automation/agents/AgentDetail'));
const AgentDeployments = lazy(() => import('@/pages/automation/agent-deployments/AgentDeployments'));
const McpServers = lazy(() => import('@/pages/automation/mcp-servers/McpServers'));
const Notifications = lazy(() => import('@/pages/settings/platform/notifications/Notifications'));
const WorkflowAlerts = lazy(() => import('@/pages/settings/platform/workflow-alerts/WorkflowAlerts'));
const Project = lazy(() => import('@/pages/automation/project/Project'));
const ProjectDeployments = lazy(() => import('@/pages/automation/project-deployments/ProjectDeployments'));
const ProjectTemplate = lazy(() => import('@/pages/automation/template/project-template/ProjectTemplate'));
const ProjectTemplates = lazy(() => import('@/pages/automation/templates/project-templates/ProjectTemplates'));
const Projects = lazy(() => import('@/pages/automation/projects/Projects'));
const Sessions = lazy(() => import('@/pages/account/settings/Sessions'));
const ApprovalTasks = lazy(() => import('@/pages/automation/approval-tasks/ApprovalTasks'));
const Chat = lazy(() => import('@/pages/automation/chats/Chat'));
const Chats = lazy(() => import('@/pages/automation/chats/Chats'));
const WorkflowTemplate = lazy(() => import('@/pages/automation/template/workflow-template/WorkflowTemplate'));
const WorkflowTemplates = lazy(() => import('@/pages/automation/templates/workflow-templates/WorkflowTemplates'));
const AssetFiles = lazy(() => import('@/pages/automation/asset-files/AssetFiles'));
const AiHub = lazy(() => import('@/ee/pages/automation/ai-hub/AiHub'));
const ContextStoreSources = lazy(() => import('@/pages/automation/context-store/ContextStoreSources'));
const ContextStores = lazy(() => import('@/pages/automation/context-store/ContextStores'));
const AiAutoMemoriesPage = lazy(() => import('@/pages/automation/ai/memories/Memories'));
const AiHubConnectorsPage = lazy(() => import('@/ee/pages/automation/ai-hub/context/AiHubConnectors'));

const AiProviders = lazy(() => import('@/ee/pages/settings/platform/ai-providers/AiProviders'));
const License = lazy(() => import('@/ee/pages/settings/platform/license/License'));
const ApiClients = lazy(() => import('@/ee/pages/automation/api-platform/api-clients/ApiClients'));
const ApiCollections = lazy(() => import('@/ee/pages/automation/api-platform/api-collections/ApiCollections'));
const Components = lazy(() => import('@/ee/pages/settings/platform/components/Components'));
const ApiConnectorManualPage = lazy(
    () => import('@/ee/pages/settings/platform/api-connectors/pages/ApiConnectorManualPage')
);
const ApiConnectorImportPage = lazy(
    () => import('@/ee/pages/settings/platform/api-connectors/pages/ApiConnectorImportPage')
);
const ApiConnectorAiPage = lazy(() => import('@/ee/pages/settings/platform/api-connectors/pages/ApiConnectorAiPage'));
const ApiConnectorEditPage = lazy(
    () => import('@/ee/pages/settings/platform/api-connectors/pages/ApiConnectorEditPage')
);
const EmbeddedApiKeys = lazy(() => import('@/ee/pages/settings/embedded/api-keys/ApiKeys'));
const EmbeddedVariables = lazy(() => import('@/ee/pages/settings/embedded/variables/Variables'));
const AppEvents = lazy(() => import('@/ee/pages/embedded/app-events/AppEvents'));
const AdminApiKeys = lazy(() => import('@/ee/pages/settings/platform/admin-api-keys/AdminApiKeys'));
const AuditEvents = lazy(() => import('@/ee/pages/settings/platform/audit-events/AuditEvents'));
const Billing = lazy(() => import('@/ee/pages/settings/platform/billing/Billing'));
const RegisteredClients = lazy(() => import('@/ee/pages/settings/platform/registered-clients/RegisteredClients'));
const IdentityProvidersPage = lazy(
    () => import('@/ee/pages/settings/platform/identity-providers/IdentityProvidersPage')
);
const AutomationWorkflows = lazy(() => import('@/ee/pages/embedded/automation-workflows/AutomationWorkflows'));
const AutomationWorkflow = lazy(() => import('@/ee/pages/embedded/automation-workflow/AutomationWorkflow'));
const ConnectedUsers = lazy(() => import('@/ee/pages/embedded/connected-users/ConnectedUsers'));
const CustomComponentDetail = lazy(
    () => import('@/ee/pages/settings/platform/custom-components/CustomComponentDetail')
);
const EmbeddedConnections = lazy(() =>
    import('@/ee/pages/embedded/connections/Connections').then((module) => ({default: module.Connections}))
);
const EmbeddedMcpServers = lazy(() => import('@/ee/pages/embedded/mcp-servers/McpServers'));
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
const AiAgents = lazy(() => import('@/ee/pages/settings/automation/ai/agents/AiAgents'));
const WorkspaceApiKeys = lazy(() => import('@/ee/pages/settings/automation/workspace-api-keys/WorkspaceApiKeys'));
const WorkspaceUsers = lazy(() => import('@/ee/pages/settings/automation/users/WorkspaceUsers'));
const WorkspaceVariables = lazy(() => import('@/ee/pages/settings/automation/variables/Variables'));
const GlobalCustomRoles = lazy(() => import('@/ee/pages/settings/platform/custom-roles/GlobalCustomRoles'));
const Workspaces = lazy(() => import('@/ee/pages/settings/automation/workspaces/Workspaces'));
const OrganizationConnections = lazy(
    () => import('@/pages/settings/platform/organization-connections/OrganizationConnections')
);
const UsersPage = lazy(() => import('@/pages/settings/platform/users/UsersPage'));

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
            // ADMIN *or* USER: a workspace admin need not be a tenant admin, and gating this on ROLE_ADMIN would
            // reinstate the very problem the page exists to solve. The page itself checks the
            // WORKSPACE_MEMBER_MANAGE scope for the current workspace.
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <WorkspaceUsers />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            // Not 'users': the tenant Users page already claims that path, and both route sets are spread into the
            // same children array under /automation/settings — so sharing it made this page shadow the tenant one,
            // and the sidebar's `href === 'users'` feature-flag filter hid this one behind a flag meant for that one.
            path: 'workspace-users',
        },
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
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <WorkspaceApiKeys />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'workspace-api-keys',
        },
        {
            // ADMIN *or* USER: a workspace admin need not be a tenant admin (WorkspaceUsers precedent above). The
            // page itself hides mutating controls when VARIABLE_MANAGE is absent, and the server enforces
            // independently.
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <WorkspaceVariables />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'variables',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <AiHubConnectorsPage />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'ai-hub/connectors',
        },
        {
            element: <Navigate replace to="/automation/settings/ai/agents/guardrails" />,
            path: 'ai/agents',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <AiAgents />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'ai/agents/:tab',
        },
    ],
    navItems: [
        {
            title: 'Current Workspace',
        },
        {
            href: 'workspace-users',
            title: 'Users',
        },
        {
            href: 'git-configuration',
            title: 'Git Configuration',
        },
        {
            href: 'workspace-api-keys',
            title: 'API Keys',
        },
        {
            href: 'variables',
            title: 'Variables',
        },
        {
            href: 'ai-hub/connectors',
            title: 'AI Hub Connectors',
        },
        {
            href: 'ai/agents',
            title: 'AI Agents',
        },
    ],
};

// Legacy settings paths kept as redirects into the merged Components page. Location-based rewrites so the
// same redirect works under both the /automation/settings and /embedded/settings mounts and preserves any
// suffix (detail ids, wizard subpaths).
const LegacyCustomComponentsRedirect = () => {
    const location = useLocation();

    return <Navigate replace to={location.pathname.replace('/custom-components', '/components/custom')} />;
};

const LegacyApiConnectorsRedirect = () => {
    const location = useLocation();

    return <Navigate replace to={location.pathname.replace('/api-connectors', '/components/api-connectors')} />;
};

// Platform settings routes
const platformSettingsRoutes = {
    children: [
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <LazyLoadWrapper>
                        <Billing />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
            path: 'billing',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <UsersPage />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'users',
        },
        {
            // Tenant-global roles are assignable in every workspace, so managing them is a tenant-wide act.
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <GlobalCustomRoles />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'global-custom-roles',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <OrganizationConnections />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'connections',
        },
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
                    <LazyLoadWrapper>
                        <McpServer />
                    </LazyLoadWrapper>
                </PrivateRoute>
            ),
            path: 'mcp-server',
        },
        {
            children: [
                {
                    element: <Navigate replace to="custom" />,
                    index: true,
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <Components tab="custom" />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'custom',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <CustomComponentDetail />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'custom/:id',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <Components tab="api-connectors" />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'api-connectors',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <Components tab="policies" />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'policies',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <Components tab="component-visibility" />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'component-visibility',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <ApiConnectorManualPage />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'api-connectors/new/manual',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <ApiConnectorImportPage />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'api-connectors/new/import',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <ApiConnectorAiPage />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'api-connectors/new/ai',
                },
                {
                    element: (
                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                            <EEVersion>
                                <LazyLoadWrapper>
                                    <ApiConnectorEditPage />
                                </LazyLoadWrapper>
                            </EEVersion>
                        </PrivateRoute>
                    ),
                    path: 'api-connectors/:apiConnectorId/edit',
                },
            ],
            path: 'components',
        },
        {
            element: <LegacyCustomComponentsRedirect />,
            path: 'custom-components/*',
        },
        {
            element: <LegacyCustomComponentsRedirect />,
            path: 'custom-components',
        },
        {
            element: <LegacyApiConnectorsRedirect />,
            path: 'api-connectors/*',
        },
        {
            element: <LegacyApiConnectorsRedirect />,
            path: 'api-connectors',
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
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <WorkflowAlerts />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'workflow-alerts',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <IdentityProvidersPage />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'identity-providers',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <AdminApiKeys />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'admin-api-keys',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <License />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'license',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <AuditEvents />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'audit-events',
        },
        {
            element: (
                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                    <EEVersion>
                        <LazyLoadWrapper>
                            <RegisteredClients />
                        </LazyLoadWrapper>
                    </EEVersion>
                </PrivateRoute>
            ),
            path: 'registered-clients',
        },
    ],
    navItems: [
        {
            title: 'Organization',
        },
        {
            href: 'users',
            title: 'Users',
        },
        {
            href: 'global-custom-roles',
            title: 'Custom Roles',
        },
        {
            href: 'connections',
            title: 'Connections',
        },
        {
            href: 'billing',
            title: 'Billing',
        },
        {
            href: 'ai-providers',
            title: 'AI Providers',
        },
        {
            href: 'mcp-server',
            title: 'MCP Server',
        },
        {
            href: 'components',
            title: 'Components',
        },
        {
            href: 'notifications',
            title: 'Notifications',
        },
        {
            href: 'workflow-alerts',
            title: 'Alerts',
        },
        {
            href: 'identity-providers',
            title: 'Identity Providers',
        },
        {
            href: 'admin-api-keys',
            title: 'Admin API Keys',
        },
        {
            href: 'license',
            title: 'License',
        },
        {
            href: 'audit-events',
            title: 'Audit Events',
        },
        {
            href: 'registered-clients',
            title: 'OAuth2 Clients',
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
        {
            element: <Login />,
            path: '/login',
        },
        {
            element: <Register />,
            path: '/register',
        },
        {
            element: <OAuth2Consent />,
            path: '/oauth2/consent',
        },
        {
            element: <OAuth2Redirect />,
            path: '/oauth2/redirect',
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
            element: <ResumeForm />,
            path: 'resume/:id',
        },
        {
            element: <TriggerForm />,
            path: 'form/:workflowExecutionId',
        },
        {
            element: <TriggerForm />,
            path: 'form/:environmentId/:workflowExecutionId',
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
                                    loader: async () => {
                                        const currentEnvironmentId = environmentStore.getState().currentEnvironmentId;

                                        if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
                                            return redirect('/automation/deployments');
                                        }

                                        return null;
                                    },
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
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <ProjectDeployments />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'deployments',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AgentDeployments />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'agent-deployments',
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
                                            <LazyLoadWrapper hasLeftSidebar>
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
                                                <A2aServers />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'a2a-servers',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <Agents />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'agents',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AgentDetail />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'agents/:agentId',
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
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <AutomationToolInvocationsPage />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'executions/tool-invocations',
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
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <DataTables />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'datatables',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <DataTable />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'datatables/:id',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <KnowledgeBases />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'knowledge-bases',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <KnowledgeBase />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'knowledge-bases/:id',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <ContextStores />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'context-stores',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <ContextStoreSources />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'context-stores/:id',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <AiHub />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'ai-hub',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <AiHub />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'ai-hub/chats/:chatId',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <AiAutoMemoriesPage />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'ai/memories',
                                },
                                {
                                    children: [
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                                    <LazyLoadWrapper hasLeftSidebar>
                                                        <Chat />
                                                    </LazyLoadWrapper>
                                                </PrivateRoute>
                                            ),
                                            path: ':workflowExecutionId',
                                        },
                                    ],
                                    element: (
                                        <LazyLoadWrapper hasLeftSidebar>
                                            <Chats />
                                        </LazyLoadWrapper>
                                    ),
                                    path: 'chats',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper>
                                                <ApprovalTasks />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'approval-tasks',
                                },
                                {
                                    loader: async () => redirect('/automation/ai/skills'),
                                    path: 'ai',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AiSkills />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'ai/skills',
                                },
                                {
                                    loader: async () => redirect('/automation/ai/skills'),
                                    path: 'ai/skills/create',
                                },
                                {
                                    loader: async () => redirect('/automation/ai/skills'),
                                    path: 'ai/skills/create/write',
                                },
                                {
                                    loader: async () => redirect('/automation/ai/skills'),
                                    path: 'ai/skills/create/upload',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AiSkills />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'ai/skills/:skillId',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AssetFiles />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'asset-files',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <LazyLoadWrapper hasLeftSidebar>
                                                <AssetFiles />
                                            </LazyLoadWrapper>
                                        </PrivateRoute>
                                    ),
                                    path: 'asset-files/:fileId',
                                },
                                {
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <AiGateway />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'ai/gateway',
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
                                                        <LazyLoadWrapper>
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
                                                ...currentWorkspaceSettingsRoutes.navItems,
                                                ...platformSettingsRoutes.navItems.slice(0, 1),
                                                {
                                                    href: '/automation/settings/workspaces',
                                                    title: 'Workspaces',
                                                },
                                                ...platformSettingsRoutes.navItems.slice(1),
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
                                    loader: async ({params}) => {
                                        // Loaded lazily like every other EE surface this router mounts - the
                                        // embedded middleware chunk stays out of the CE bundle graph.
                                        const [{IntegrationApi}, {IntegrationKeys}] = await Promise.all([
                                            import('@/ee/shared/middleware/embedded/configuration'),
                                            import('@/ee/shared/queries/embedded/integrations.queries'),
                                        ]);

                                        return queryClient.ensureQueryData({
                                            queryFn: () =>
                                                new IntegrationApi().getIntegration({
                                                    id: parseInt(params.integrationId!),
                                                }),
                                            queryKey: IntegrationKeys.integration(parseInt(params.integrationId!)),
                                        });
                                    },
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
                                                    <AutomationWorkflow />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'automation-workflows/:workflowId/editor',
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
                                                <LazyLoadWrapper hasLeftSidebar>
                                                    <ToolInvocationsPage basePath="/embedded/executions" />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'executions/tool-invocations',
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
                                    element: (
                                        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]}>
                                            <EEVersion>
                                                <LazyLoadWrapper>
                                                    <EmbeddedMcpServers />
                                                </LazyLoadWrapper>
                                            </EEVersion>
                                        </PrivateRoute>
                                    ),
                                    path: 'mcp-servers',
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
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                                                    <EEVersion>
                                                        <LazyLoadWrapper>
                                                            <EmbeddedApiKeys />
                                                        </LazyLoadWrapper>
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'api-keys',
                                        },
                                        {
                                            element: (
                                                <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
                                                    <EEVersion>
                                                        <LazyLoadWrapper>
                                                            <EmbeddedVariables />
                                                        </LazyLoadWrapper>
                                                    </EEVersion>
                                                </PrivateRoute>
                                            ),
                                            path: 'variables',
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
                                                {
                                                    href: '/embedded/settings/api-keys',
                                                    title: 'API Keys',
                                                },
                                                {
                                                    href: '/embedded/settings/variables',
                                                    title: 'Variables',
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
            element: (
                <Suspense fallback={null}>
                    <App />
                </Suspense>
            ),
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
