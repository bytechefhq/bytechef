import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectListItem from '../ProjectListItem';

const ALL_SCOPES = [
    'DEPLOYMENT_CREATE',
    'PROJECT_CREATE',
    'PROJECT_DELETE',
    'PROJECT_PUBLISH',
    'PROJECT_PULL',
    'PROJECT_SETTINGS',
    'WORKFLOW_CREATE',
    'WORKFLOW_EDIT',
    'WORKSPACE_MANAGE',
];

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({cancelQueries: vi.fn(), invalidateQueries: vi.fn(), removeQueries: vi.fn()}),
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.mutations', () => ({
    usePullProjectFromGitMutation: () => ({mutate: vi.fn()}),
    useUpdateProjectGitConfigurationMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.queries', () => ({
    ProjectGitConfigurationKeys: {projectGitConfigurations: ['projectGitConfigurations']},
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({triggerNode}: {triggerNode?: ReactNode}) => <>{triggerNode}</>,
}));

vi.mock('@/pages/automation/project/components/ProjectGitConfigurationDialog', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/project/components/ProjectShareDialog', () => ({
    ProjectShareDialog: () => null,
}));

vi.mock('@/pages/automation/project/hooks/useConverterN8nToWorkflow', () => ({
    useConvertN8nToWorkflow: () => ({convertN8nWorkflow: vi.fn()}),
}));

vi.mock('@/pages/automation/project/utils/handleImportN8nWorkflow', () => ({default: vi.fn()}));

vi.mock('@/pages/automation/project/utils/handleImportWorkflow', () => ({default: vi.fn()}));

vi.mock('@/pages/automation/projects/components/ProjectPublishDialog', () => ({default: () => null}));

vi.mock('@/pages/automation/projects/components/ProjectDialog', () => ({default: () => null}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({default: () => null}));

vi.mock('@/shared/edition/EEVersion', () => ({
    default: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectWorkflowCreated: vi.fn(), captureProjectWorkflowImported: vi.fn()}),
}));

vi.mock('@/shared/hooks/useHasEnabledAiProvider', () => ({
    useHasEnabledAiProvider: () => ({hasEnabledAiProvider: true, isPending: false}),
}));

vi.mock('@/shared/mutations/automation/projectTags.mutations', () => ({
    useUpdateProjectTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    useDeleteProjectMutation: () => ({mutate: vi.fn()}),
    useDuplicateProjectMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useCreateProjectWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectCategories.queries', () => ({
    ProjectCategoryKeys: {projectCategories: (workspaceId: number) => ['projectCategories', workspaceId]},
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: [], isFetching: false, refetch: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectTags.queries', () => ({
    ProjectTagKeys: {projectTags: ['projectTags']},
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {project: (id: number) => ['projects', id], projects: ['projects']},
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => undefined,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => (
        <div data-read-only={readOnly ? 'true' : 'false'} data-testid="tag-list" />
    ),
}));

const project: Project = {
    id: 1,
    lastProjectVersion: 2,
    lastPublishedDate: new Date('2024-01-15T10:00:00'),
    name: 'Test Project',
    projectWorkflowIds: [],
    tags: [],
    workspaceId: 1049,
};

const renderProjectListItem = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <Collapsible>
                    <ProjectListItem project={project} />
                </Collapsible>
            </TooltipProvider>
        </MemoryRouter>
    );

const openProjectActionsMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'More Project Actions'}));
};

beforeEach(() => {
    hoistedScope.grantedScopes = [...ALL_SCOPES];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ProjectListItem', () => {
    it('shows every gated control when all scopes are granted', async () => {
        renderProjectListItem();

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Workflow'})).toBeInTheDocument();
        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');

        await openProjectActionsMenu();

        for (const name of [
            'Publish Project',
            'Edit Project',
            'Duplicate Project',
            'Share Project',
            'Export Project',
            'Pull Project from Git',
            'Git Configuration',
            'Delete Project',
        ]) {
            expect(screen.getByRole('menuitem', {name})).toBeInTheDocument();
        }
    });

    it('hides every gated control when no scope is granted, leaving Export', async () => {
        hoistedScope.grantedScopes = [];

        renderProjectListItem();

        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Workflow'})).not.toBeInTheDocument();
        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');

        await openProjectActionsMenu();

        expect(screen.getByRole('menuitem', {name: 'Export Project'})).toBeInTheDocument();

        for (const name of [
            'Publish Project',
            'Edit Project',
            'Duplicate Project',
            'Share Project',
            'Pull Project from Git',
            'Git Configuration',
            'Delete Project',
        ]) {
            expect(screen.queryByRole('menuitem', {name})).not.toBeInTheDocument();
        }
    });

    it('hides Publish and Deploy when WORKFLOW_EDIT is missing even with PROJECT_PUBLISH and DEPLOYMENT_CREATE', async () => {
        hoistedScope.grantedScopes = ALL_SCOPES.filter((scope) => scope !== 'WORKFLOW_EDIT');

        renderProjectListItem();

        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();

        await openProjectActionsMenu();

        expect(screen.queryByRole('menuitem', {name: 'Publish Project'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Edit Project'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete Project'})).toBeInTheDocument();
    });
});
