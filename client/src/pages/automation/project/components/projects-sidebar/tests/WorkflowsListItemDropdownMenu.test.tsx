import WorkflowsListItemDropdownMenu from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItemDropdownMenu';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    duplicateMutate: vi.fn(),
    featureFlagEnabled: false,
    navigate: vi.fn(),
    toast: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [new URLSearchParams(), vi.fn()],
}));

vi.mock('sonner', () => ({
    toast: hoisted.toast,
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useDeleteWorkflowMutation: (mutationProps?: {onSuccess?: () => void}) => ({
        mutate: (...args: unknown[]) => {
            hoisted.deleteMutate(...args);

            mutationProps?.onSuccess?.();
        },
    }),
    useDuplicateWorkflowMutation: (mutationProps?: {onError?: () => void; onSuccess?: () => void}) => ({
        mutate: (...args: unknown[]) => {
            hoisted.duplicateMutate(...args);

            mutationProps?.onError?.();
            mutationProps?.onSuccess?.();
        },
    }),
    useUpdateWorkflowMutation: (mutationProps?: {onSuccess?: () => void}) => ({
        mutate: (...args: unknown[]) => {
            hoisted.updateMutate(...args);

            mutationProps?.onSuccess?.();
        },
    }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => hoisted.featureFlagEnabled,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => 'https://templates.example.com',
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: ({
        onClose,
        onSave,
        updateWorkflowMutation,
    }: {
        onClose: () => void;
        onSave: () => void;
        updateWorkflowMutation: {mutate: (values: unknown) => void};
    }) => (
        <div>
            <span>Workflow dialog</span>

            <button onClick={onSave} type="button">
                Save workflow
            </button>

            <button onClick={() => updateWorkflowMutation.mutate({id: 'workflow-1'})} type="button">
                Update workflow
            </button>

            <button onClick={onClose} type="button">
                Close workflow dialog
            </button>
        </div>
    ),
}));

vi.mock('@/pages/automation/project/components/WorkflowShareDialog', () => ({
    WorkflowShareDialog: ({onOpenChange}: {onOpenChange: () => void}) => (
        <div>
            <span>Workflow share dialog</span>

            <button onClick={onOpenChange} type="button">
                Close share dialog
            </button>
        </div>
    ),
}));

const project = {
    id: 50,
    lastProjectVersion: 3,
    name: 'Project One',
    projectWorkflowIds: [10, 11],
    workspaceId: 1,
};

const workflow = {
    id: 'workflow-1',
    label: 'Workflow One',
    projectWorkflowId: 10,
    workflowUuid: 'uuid-1',
};

const renderMenu = (currentWorkflowId = 'workflow-other', projectOverrides = {}) =>
    render(
        <WorkflowsListItemDropdownMenu
            currentWorkflowId={currentWorkflowId}
            project={{...project, ...projectOverrides}}
            workflow={workflow}
        />
    );

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));
};

const deleteWorkflow = async () => {
    await openMenu();
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
    await userEvent.click(screen.getByRole('button', {name: 'Delete'}));
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.featureFlagEnabled = false;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkflowsListItemDropdownMenu', () => {
    it('offers Edit, Duplicate, Share, Export and Delete', async () => {
        renderMenu();

        await openMenu();

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent?.trim())).toEqual([
            'Edit',
            'Duplicate',
            'Share',
            'Export',
            'Delete',
        ]);
    });

    it('opens the community template form when ff-2939 is on', async () => {
        hoisted.featureFlagEnabled = true;

        const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Share with Community'}));

        expect(openSpy).toHaveBeenCalledWith('https://templates.example.com', '_blank');

        openSpy.mockRestore();
    });

    it('exports through the automation endpoint', async () => {
        const location = {href: ''};

        Object.defineProperty(window, 'location', {configurable: true, value: location, writable: true});

        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Export'}));

        expect(location.href).toBe('/api/automation/internal/workflows/workflow-1/export');
    });

    it('duplicates into the same project and reports success', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Duplicate'}));

        expect(hoisted.duplicateMutate).toHaveBeenCalledWith({id: 50, workflowId: 'workflow-1'});
        expect(hoisted.toast).toHaveBeenCalledWith('Workflow duplicated successfully.');
    });

    it('closes the edit dialog once the workflow is updated', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save workflow'}));
        await userEvent.click(screen.getByRole('button', {name: 'Update workflow'}));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({id: 'workflow-1'});
        expect(screen.queryByText('Workflow dialog')).not.toBeInTheDocument();
    });

    it('closes the edit dialog when it is dismissed', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));
        await userEvent.click(screen.getByRole('button', {name: 'Close workflow dialog'}));

        expect(screen.queryByText('Workflow dialog')).not.toBeInTheDocument();
    });

    it('opens and closes the share dialog', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Share'}));

        expect(screen.getByText('Workflow share dialog')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close share dialog'}));

        expect(screen.queryByText('Workflow share dialog')).not.toBeInTheDocument();
    });

    it('navigates to the first remaining workflow when the open one is deleted', async () => {
        renderMenu('workflow-1');

        await deleteWorkflow();

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'workflow-1'});
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects/50/project-workflows/11?');
    });

    it('returns to the project list when the last workflow is deleted', async () => {
        renderMenu('workflow-1', {projectWorkflowIds: [10]});

        await deleteWorkflow();

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects');
    });

    it('stays put when a workflow other than the open one is deleted', async () => {
        renderMenu('workflow-other');

        await deleteWorkflow();

        expect(hoisted.navigate).not.toHaveBeenCalled();
    });
});
