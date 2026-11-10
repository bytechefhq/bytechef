import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectWorkflowListItem from '@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowListItem';
import {render, screen, userEvent} from '@/shared/util/test-utils';
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

const mockApplicationInfoState = vi.hoisted(() => ({templatesSubmissionForm: {workflows: ''}}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: typeof mockApplicationInfoState) => unknown) =>
        selector(mockApplicationInfoState),
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

vi.mock('@/pages/automation/project/components/WorkflowShareDialog', () => ({
    WorkflowShareDialog: () => <div data-testid="workflow-share-dialog" />,
}));

vi.mock('@/shared/components/DeleteWorkflowAlertDialog', () => ({
    default: ({onClose}: {onClose: () => void}) => (
        <div data-testid="delete-workflow-dialog">
            <button onClick={onClose} type="button">
                Dismiss delete dialog
            </button>
        </div>
    ),
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

        mockApplicationInfoState.templatesSubmissionForm.workflows = '';
    });

    it('should render workflow label', () => {
        renderProjectWorkflowListItem();

        expect(screen.getByText('Test Workflow')).toBeInTheDocument();
    });

    it('should render last modified date', () => {
        renderProjectWorkflowListItem();

        expect(screen.getAllByText(/Modified at/).length).toBeGreaterThan(0);
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

    describe('menu', () => {
        const openMenu = async () => {
            renderProjectWorkflowListItem();

            await userEvent.click(document.querySelector('[aria-haspopup="menu"]')!);
        };

        it('should render with the design-system DropdownMenu', async () => {
            await openMenu();

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
        });

        it('should open the edit dialog when Edit is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

            expect(screen.getByTestId('workflow-dialog')).toBeInTheDocument();
        });

        it('should duplicate the workflow when Duplicate is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Duplicate'}));

            expect(mockDuplicateMutate).toHaveBeenCalledWith({id: 1, workflowId: 'workflow-1'});
        });

        it('should open the share dialog when Share is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Share'}));

            expect(screen.getByTestId('workflow-share-dialog')).toBeInTheDocument();
        });

        it('should not render Share with Community without a submission form', async () => {
            await openMenu();

            expect(screen.queryByRole('menuitem', {name: 'Share with Community'})).not.toBeInTheDocument();
        });

        it('should open the submission form when Share with Community is clicked', async () => {
            mockApplicationInfoState.templatesSubmissionForm.workflows = 'https://example.com/form';

            const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Share with Community'}));

            expect(openSpy).toHaveBeenCalledWith('https://example.com/form', '_blank');

            openSpy.mockRestore();
        });

        it('should render an Export row', async () => {
            await openMenu();

            expect(screen.getByRole('menuitem', {name: 'Export'})).toBeInTheDocument();
        });
    });

    describe('delete dialog', () => {
        it('should close the delete dialog when it is dismissed', async () => {
            renderProjectWorkflowListItem();

            await userEvent.click(document.querySelector('[aria-haspopup="menu"]')!);

            await userEvent.click(screen.getByRole('menuitem', {name: /Delete/}));

            expect(screen.getByTestId('delete-workflow-dialog')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Dismiss delete dialog'}));

            expect(screen.queryByTestId('delete-workflow-dialog')).not.toBeInTheDocument();
            expect(mockDeleteMutate).not.toHaveBeenCalled();
        });
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
