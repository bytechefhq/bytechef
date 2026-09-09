import AutomationWorkflowEditorWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItemDropdownMenu';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    duplicateMutate: vi.fn(),
    navigate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAutomationWorkflowProjectWorkflowMutation: () => ({
        mutate: (variables: unknown, options?: {onSuccess?: () => void}) => {
            hoisted.deleteMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useDuplicateAutomationWorkflowProjectWorkflowMutation: () => ({
        mutate: (variables: unknown, options?: {onSuccess?: () => void}) => {
            hoisted.duplicateMutate(variables);

            options?.onSuccess?.();
        },
    }),
    useUpdateAutomationWorkflowProjectWorkflowMutation: () => ({
        mutate: (variables: unknown, options?: {onSuccess?: () => void}) => {
            hoisted.updateMutate(variables);

            options?.onSuccess?.();
        },
    }),
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog',
    () => ({
        default: ({
            onClose,
            onSubmit,
        }: {
            onClose: () => void;
            onSubmit: (values: {description: string; label: string}) => void;
        }) => (
            <div>
                <span>Automation workflow dialog</span>

                <button onClick={() => onSubmit({description: 'Renamed description', label: 'Renamed'})} type="button">
                    Submit workflow
                </button>

                <button onClick={onClose} type="button">
                    Close workflow dialog
                </button>
            </div>
        ),
    })
);

const workflow = {
    components: [],
    description: 'Sends mail',
    label: 'Workflow One',
    lastModifiedDate: '2026-02-11T09:30:00Z',
    triggers: [],
    workflowUuid: 'uuid-1',
};

const project = {
    categoryId: null,
    description: null,
    id: 'project-1',
    lastPublishedVersion: null,
    name: 'Project One',
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [workflow, {...workflow, label: 'Workflow Two', workflowUuid: 'uuid-2'}],
};

const renderMenu = (currentWorkflowId = 'uuid-other', projectOverrides = {}) =>
    render(
        <AutomationWorkflowEditorWorkflowsListItemDropdownMenu
            currentWorkflowId={currentWorkflowId}
            project={{...project, ...projectOverrides}}
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

describe('AutomationWorkflowEditorWorkflowsListItemDropdownMenu', () => {
    it('offers Edit, Duplicate and Delete', async () => {
        renderMenu();

        await openMenu();

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent?.trim())).toEqual([
            'Edit',
            'Duplicate',
            'Delete',
        ]);
    });

    it('duplicates by workflow uuid', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Duplicate'}));

        expect(hoisted.duplicateMutate).toHaveBeenCalledWith({workflowUuid: 'uuid-1'});
    });

    it('opens the edit dialog from Edit', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByText('Automation workflow dialog')).toBeInTheDocument();
    });

    it('renames the workflow and closes the dialog', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));
        await userEvent.click(screen.getByRole('button', {name: 'Submit workflow'}));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            description: 'Renamed description',
            label: 'Renamed',
            workflowUuid: 'uuid-1',
        });
        expect(screen.queryByText('Automation workflow dialog')).not.toBeInTheDocument();
    });

    it('closes the edit dialog when it is dismissed', async () => {
        renderMenu();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));
        await userEvent.click(screen.getByRole('button', {name: 'Close workflow dialog'}));

        expect(screen.queryByText('Automation workflow dialog')).not.toBeInTheDocument();
    });

    it('returns to the workflow list when the last workflow is deleted', async () => {
        renderMenu('uuid-1', {workflowTemplates: [workflow]});

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows');
    });

    it('navigates to the first remaining workflow when the open one is deleted', async () => {
        renderMenu('uuid-1');

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({workflowUuid: 'uuid-1'});
        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows/uuid-2/editor');
    });

    it('stays put when a workflow other than the open one is deleted', async () => {
        renderMenu('uuid-other');

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.navigate).not.toHaveBeenCalled();
    });
});
