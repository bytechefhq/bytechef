import IntegrationWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItemDropdownMenu';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    navigate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [new URLSearchParams(), vi.fn()],
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useDeleteWorkflowMutation: (mutationProps?: {onSuccess?: () => void}) => ({
        mutate: (...args: unknown[]) => {
            hoisted.deleteMutate(...args);

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

const integration = {
    componentName: 'accelo',
    id: 100,
    integrationWorkflowIds: [10, 11],
    multipleInstances: false,
    name: 'Integration One',
};

const workflow = {
    id: 'workflow-1',
    integrationWorkflowId: 10,
    label: 'Workflow One',
};

const renderMenu = (currentWorkflowId = 'workflow-other', integrationOverrides = {}) =>
    render(
        <IntegrationWorkflowsListItemDropdownMenu
            currentWorkflowId={currentWorkflowId}
            integration={{...integration, ...integrationOverrides}}
            workflow={workflow}
        />
    );

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IntegrationWorkflowsListItemDropdownMenu', () => {
    it('offers Edit, Export and Delete', async () => {
        renderMenu();

        await openMenu();

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent?.trim())).toEqual([
            'Edit',
            'Export',
            'Delete',
        ]);
    });

    it('opens the edit dialog from Edit', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByText('Workflow dialog')).toBeInTheDocument();
    });

    it('deletes the workflow once the confirmation is accepted', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: 'workflow-1'});
    });

    it('stays put when a workflow other than the open one is deleted', async () => {
        renderMenu('workflow-other');

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.navigate).not.toHaveBeenCalled();
    });

    it('exports through the embedded endpoint', async () => {
        const location = {href: ''};

        Object.defineProperty(window, 'location', {configurable: true, value: location, writable: true});

        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Export'}));

        expect(location.href).toBe('/api/embedded/internal/workflows/workflow-1/export');
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

    it('returns to the integration list when the last workflow is deleted', async () => {
        renderMenu('workflow-1', {integrationWorkflowIds: [10]});

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations');
    });

    it('navigates to the first remaining workflow when the open one is deleted', async () => {
        renderMenu('workflow-1');

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/100/integration-workflows/11?');
    });
});
