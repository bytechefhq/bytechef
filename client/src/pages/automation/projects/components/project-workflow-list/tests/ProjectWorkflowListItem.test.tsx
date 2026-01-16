import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectWorkflowListItem from '@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowListItem';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockInvalidateQueries = vi.fn();

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({
            invalidateQueries: mockInvalidateQueries,
        }),
    };
});

vi.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: React.ReactNode; to: string}) => <a href={to}>{children}</a>,
    useSearchParams: () => [new URLSearchParams(''), vi.fn()],
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => null,
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

const mockDeleteMutate = vi.fn();
const mockDuplicateMutate = vi.fn();
const mockUpdateMutate = vi.fn();

let deleteOnSuccess: () => void;
let duplicateOnSuccess: () => void;
let duplicateOnError: () => void;
let updateOnSuccess: () => void;

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useDeleteWorkflowMutation: ({onSuccess}: {onSuccess: () => void}) => {
        deleteOnSuccess = onSuccess;

        return {mutate: mockDeleteMutate};
    },
    useDuplicateWorkflowMutation: ({onError, onSuccess}: {onError: () => void; onSuccess: () => void}) => {
        duplicateOnSuccess = onSuccess;
        duplicateOnError = onError;

        return {mutate: mockDuplicateMutate};
    },
    useUpdateWorkflowMutation: ({onSuccess}: {onSuccess: () => void}) => {
        updateOnSuccess = onSuccess;

        return {mutate: mockUpdateMutate};
    },
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    WorkflowKeys: {
        workflow: (id: string) => ['workflow', id],
    },
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {
        projects: ['projects'],
    },
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    ProjectWorkflowKeys: {
        projectWorkflows: (projectId: number) => ['projectWorkflows', projectId],
    },
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    WorkflowTestConfigurationKeys: {
        workflowTestConfiguration: (workflowId: string) => ['workflowTestConfiguration', workflowId],
    },
}));

vi.mock('@/shared/components/WorkflowComponentsList', () => ({
    default: () => <div data-testid="workflow-components-list" />,
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: () => <div data-testid="workflow-dialog" />,
}));

vi.mock('@/shared/components/DeleteWorkflowAlertDialog', () => ({
    default: () => <div data-testid="delete-workflow-dialog" />,
}));

const mockProject = {
    id: 1,
    lastProjectVersion: 1,
    name: 'Test Project',
    workspaceId: 1,
};

const mockWorkflow = {
    id: 'workflow-1',
    label: 'Test Workflow',
    lastModifiedDate: new Date('2024-01-15T10:00:00'),
    projectWorkflowId: 101,
    workflowUuid: 'uuid-123',
};

const renderProjectWorkflowListItem = () => {
    return render(
        <TooltipProvider>
            <ProjectWorkflowListItem
                project={mockProject}
                workflow={mockWorkflow}
                workflowComponentDefinitions={{}}
                workflowTaskDispatcherDefinitions={{}}
            />
        </TooltipProvider>
    );
};

describe('ProjectWorkflowListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render workflow label', () => {
        renderProjectWorkflowListItem();

        expect(screen.getByText('Test Workflow')).toBeInTheDocument();
    });

    it('should render last modified date', () => {
        renderProjectWorkflowListItem();

        expect(screen.getByText(/Modified at/)).toBeInTheDocument();
    });

    it('should render workflow components list', () => {
        renderProjectWorkflowListItem();

        expect(screen.getByTestId('workflow-components-list')).toBeInTheDocument();
    });

    it('should render link to workflow', () => {
        renderProjectWorkflowListItem();

        const link = screen.getByRole('link');

        expect(link).toHaveAttribute('href', '/automation/projects/1/project-workflows/101?');
    });

    describe('deleteWorkflowMutation', () => {
        it('should invalidate projects query on success', () => {
            renderProjectWorkflowListItem();

            deleteOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projects']});
        });
    });

    describe('duplicateWorkflowMutation', () => {
        it('should invalidate projects query on success', () => {
            renderProjectWorkflowListItem();

            duplicateOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projects']});
        });

        it('should invalidate projects query on error', () => {
            renderProjectWorkflowListItem();

            duplicateOnError();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projects']});
        });
    });

    describe('updateWorkflowMutation', () => {
        it('should invalidate projects query on success', () => {
            renderProjectWorkflowListItem();

            updateOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projects']});
        });

        it('should invalidate projectWorkflows query on success', () => {
            renderProjectWorkflowListItem();

            updateOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projectWorkflows', 1]});
        });

        it('should invalidate workflowTestConfiguration query on success', () => {
            renderProjectWorkflowListItem();

            updateOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['workflowTestConfiguration', 'workflow-1'],
            });
        });

        it('should invalidate workflow query on success', () => {
            renderProjectWorkflowListItem();

            updateOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['workflow', 'workflow-1']});
        });

        it('should invalidate all required queries on success', () => {
            renderProjectWorkflowListItem();

            updateOnSuccess();

            expect(mockInvalidateQueries).toHaveBeenCalledTimes(4);
            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projects']});
            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['projectWorkflows', 1]});
            expect(mockInvalidateQueries).toHaveBeenCalledWith({
                queryKey: ['workflowTestConfiguration', 'workflow-1'],
            });
            expect(mockInvalidateQueries).toHaveBeenCalledWith({queryKey: ['workflow', 'workflow-1']});
        });
    });
});
