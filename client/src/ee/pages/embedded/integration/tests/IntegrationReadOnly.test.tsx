import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorHeader from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/AutomationWorkflowEditorHeader';
import Integration from '@/ee/pages/embedded/integration/Integration';
import IntegrationHeader from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeader';
import {useEmbeddedWorkflowReadOnly} from '@/ee/pages/embedded/shared/hooks/useEmbeddedWorkflowReadOnly';
import WorkflowEditorReadOnlyProvider from '@/pages/platform/workflow-editor/providers/WorkflowEditorReadOnlyProvider';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {isWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/utils/workflowEditorReadOnlyGuard';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/embedded/integration/hooks/useIntegration', () => ({
    useIntegration: () => ({
        bottomResizablePanelRef: {current: null},
        cancelWorkflowQueries: vi.fn(),
        deleteClusterElementParameterMutation: {},
        deleteWorkflowNodeParameterMutation: {},
        handleIntegrationClick: vi.fn(),
        handleWorkflowExecutionsTestOutputCloseClick: vi.fn(),
        integrationId: 5,
        integrationWorkflowId: 11,
        invalidateWorkflowQueries: vi.fn(),
        leftSidebarOpen: false,
        sidebarLoaded: false,
        updateClusterElementParameterMutation: {},
        updateWorkflowEditorMutation: {},
        updateWorkflowMutation: {},
        updateWorkflowNodeParameterMutation: {},
        useGetConnectionsQuery: () => ({data: []}),
    }),
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/hooks/useIntegrationHeader', () => ({
    useIntegrationHeader: () => ({
        handleIntegrationWorkflowValueChange: vi.fn(),
        handlePublishIntegrationSubmit: vi.fn(),
        handleRunClick: vi.fn(),
        handleShowOutputClick: vi.fn(),
        handleStopClick: vi.fn(),
        integration: {id: 5, name: 'Gmail'},
        integrationWorkflows: [{integrationWorkflowId: 11, label: 'Workflow 1'}],
        publishIntegrationMutationIsPending: false,
    }),
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/hooks/useAutomationWorkflowEditorHeader',
    () => ({
        useAutomationWorkflowEditorHeader: () => ({
            handlePublishProjectSubmit: vi.fn(),
            handleRunClick: vi.fn(),
            handleShowOutputClick: vi.fn(),
            handleStopClick: vi.fn(),
            handleWorkflowValueChange: vi.fn(),
            project: {id: 'project-1', name: 'Mailing', workflowTemplates: []},
            publishProjectMutationIsPending: false,
        }),
    })
);

vi.mock('@/ee/pages/embedded/automation-workflow/hooks/useCreateAutomationWorkflowProjectWorkflow', () => ({
    useCreateAutomationWorkflowProjectWorkflow: () => ({
        createWorkflow: vi.fn(),
        handleWorkflowFileChange: vi.fn(),
        workflowFileInputRef: {current: null},
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useAutomationWorkflowProjectCategoriesQuery: () => ({data: {automationWorkflowProjectCategories: []}}),
    useAutomationWorkflowProjectTagsQuery: () => ({data: {automationWorkflowProjectTags: []}}),
    useAutomationWorkflowProjectsQuery: () => ({data: undefined}),
    useDeleteAutomationWorkflowProjectMutation: () => ({mutate: vi.fn()}),
    useDeleteAutomationWorkflowProjectWorkflowMutation: () => ({mutate: vi.fn()}),
    useDuplicateAutomationWorkflowProjectMutation: () => ({mutate: vi.fn()}),
    useDuplicateAutomationWorkflowProjectWorkflowMutation: () => ({mutate: vi.fn()}),
    useUpdateAutomationWorkflowProjectMutation: () => ({mutate: vi.fn()}),
    useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useRun', () => ({
    useRun: () => ({runDisabled: false, runDisabledReasons: []}),
}));

vi.mock('@/shared/hooks/useWorkflowTestRunGuard', () => ({
    useWorkflowTestRunGuard: () => ({
        cancelLeave: vi.fn(),
        confirmLeave: vi.fn(),
        showLeaveDialog: false,
        workflowIsRunning: false,
        workflowTestExecution: undefined,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/WorkflowEditorLayout', () => ({
    default: function WorkflowEditorLayoutMock() {
        const readOnly = useWorkflowEditorReadOnly();

        return <div data-read-only={String(readOnly)} data-testid="workflow-editor-layout" />;
    },
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput', () => ({default: () => null}));

vi.mock('@/shared/components/WorkflowTestRunLeaveDialog', () => ({default: () => null}));

vi.mock('@/ee/pages/embedded/integration/components/integrations-sidebar/IntegrationsLeftSidebar', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb', () => ({
    default: ({itemSelect}: {itemSelect: ReactNode}) => <nav>{itemSelect}</nav>,
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorBreadcrumb',
    () => ({default: ({itemSelect}: {itemSelect: ReactNode}) => <nav>{itemSelect}</nav>})
);

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowSelect',
    () => ({default: () => null})
);

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowProjectVersionHistorySheet',
    () => ({default: () => null})
);

vi.mock('@/pages/platform/workflow-editor/components/WorkflowActionsButton', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => <button data-read-only={String(!!readOnly)}>Test</button>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/PublishPopover', () => ({
    default: () => <button>Publish</button>,
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/PublishPopover',
    () => ({default: () => <button>Publish</button>})
);

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/OutputButton', () => ({
    default: () => <button>Output</button>,
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/OutputButton',
    () => ({default: () => <button>Output</button>})
);

vi.mock('@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/SettingsMenu', () => ({
    default: () => <button>Settings</button>,
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorSettingsMenu',
    () => ({default: () => <button>Settings</button>})
);

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

const setDevelopmentScopes = (scopes: string[], unionScopes: string[] = scopes) => {
    act(() => {
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes, status: 'loaded'},
                [UNION_ENVIRONMENT_ID]: {scopes: unionScopes, status: 'loaded'},
            },
        });
    });
};

const EditorReadOnlyBoundary = ({children}: {children: ReactNode}) => (
    <WorkflowEditorReadOnlyProvider readOnly={useEmbeddedWorkflowReadOnly()}>{children}</WorkflowEditorReadOnlyProvider>
);

const renderHubHeader = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <EditorReadOnlyBoundary>
                    <AutomationWorkflowEditorHeader
                        bottomResizablePanelRef={{current: null}}
                        currentWorkflowId="workflow-1"
                        projectId="project-1"
                        runDisabled={false}
                        updateWorkflowMutation={{} as UpdateWorkflowMutationType}
                    />
                </EditorReadOnlyBoundary>
            </TooltipProvider>
        </MemoryRouter>
    );

const renderIntegrationHeader = () =>
    render(
        <TooltipProvider>
            <EditorReadOnlyBoundary>
                <IntegrationHeader
                    bottomResizablePanelRef={{current: null}}
                    integrationId={5}
                    integrationWorkflowId={11}
                    runDisabled={false}
                    updateWorkflowMutation={{} as UpdateWorkflowMutationType}
                />
            </EditorReadOnlyBoundary>
        </TooltipProvider>
    );

describe('IntegrationReadOnly', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'member'} as never,
            authenticated: true,
        });
        environmentStore.setState({currentEnvironmentId: 1});
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
    });

    afterEach(() => {
        authenticationStore.setState({account: undefined, authenticated: false});
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
    });

    it('testTheIntegrationEditorOpensReadOnlyWithoutDevelopmentWorkflowEdit', () => {
        setDevelopmentScopes(['WORKFLOW_VIEW'], ['INTEGRATION_PUBLISH', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        const {unmount} = render(
            <MemoryRouter>
                <TooltipProvider>
                    <Integration />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(screen.getByTestId('workflow-editor-layout')).toHaveAttribute('data-read-only', 'true');
        expect(isWorkflowEditorReadOnly()).toBe(true);
        expect(screen.getByRole('button', {name: 'Test'})).toHaveAttribute('data-read-only', 'true');
        expect(screen.getByRole('button', {name: 'Publish'})).toBeInTheDocument();

        unmount();

        setDevelopmentScopes(
            ['WORKFLOW_EDIT', 'WORKFLOW_VIEW'],
            ['INTEGRATION_PUBLISH', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']
        );

        render(
            <MemoryRouter>
                <TooltipProvider>
                    <Integration />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(screen.getByTestId('workflow-editor-layout')).toHaveAttribute('data-read-only', 'false');
        expect(screen.getByRole('button', {name: 'Test'})).toHaveAttribute('data-read-only', 'false');
        expect(screen.getByRole('button', {name: 'Publish'})).toBeInTheDocument();
        expect(screen.queryByLabelText('View only')).not.toBeInTheDocument();
    });

    it('testTheViewOnlyBadgeShowsInBothEmbeddedEditors', () => {
        setDevelopmentScopes(['WORKFLOW_VIEW'], ['INTEGRATION_PUBLISH', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        const integrationHeader = renderIntegrationHeader();

        expect(screen.getByLabelText('View only')).toHaveTextContent('View only');

        integrationHeader.unmount();

        const hubHeader = renderHubHeader();

        expect(screen.getByLabelText('View only')).toHaveTextContent('View only');
        expect(screen.getByRole('button', {name: 'Test'})).toHaveAttribute('data-read-only', 'true');
        expect(screen.getByRole('button', {name: 'Publish'})).toBeInTheDocument();

        hubHeader.unmount();

        setDevelopmentScopes(
            ['WORKFLOW_EDIT', 'WORKFLOW_VIEW'],
            ['INTEGRATION_PUBLISH', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']
        );

        renderHubHeader();

        expect(screen.queryByLabelText('View only')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Publish'})).toBeInTheDocument();
    });
});
