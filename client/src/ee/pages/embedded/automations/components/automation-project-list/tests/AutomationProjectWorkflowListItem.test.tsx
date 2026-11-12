import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationProjectWorkflowListItem from '@/ee/pages/embedded/automations/components/automation-project-list/AutomationProjectWorkflowListItem';
import {resetAll, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mocks = vi.hoisted(() => ({
    duplicateMutate: vi.fn(),
    duplicateMutationOptions: undefined as {onSuccess?: () => void} | undefined,
    invalidateQueries: vi.fn(),
    toast: vi.fn(),
}));

vi.mock('sonner', () => ({toast: mocks.toast}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDuplicateAutomationProjectWorkflowMutation: (options?: {onSuccess?: () => void}) => {
        mocks.duplicateMutationOptions = options;

        return {isPending: false, mutate: mocks.duplicateMutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

const handlers = {
    onDeleteWorkflow: vi.fn(),
    onEditWorkflow: vi.fn(),
    onSelectWorkflow: vi.fn(),
};

const workflow = {
    components: [{icon: '<svg />', name: 'gmail', title: 'Gmail'}],
    description: 'Sends mail',
    label: 'Mailer',
    lastModifiedDate: '2026-02-11T09:30:00Z',
    permissionExpression: null,
    triggers: [{icon: '<svg />', name: 'manual', title: 'Manual'}],
    workflowId: 'draft-wf-1',
    workflowUuid: 'wf-1',
};

const renderRow = () =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <TooltipProvider>
                <ul>
                    <AutomationProjectWorkflowListItem {...handlers} workflow={workflow} />
                </ul>
            </TooltipProvider>
        </QueryClientProvider>
    );

const openMenu = async () => {
    renderRow();

    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Mailer'}));
};

describe('AutomationProjectWorkflowListItem', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('renders the label, the trigger badge, and the modified date', () => {
        renderRow();

        expect(screen.getByText('Mailer')).toBeInTheDocument();
        expect(screen.getByText('Manual')).toBeInTheDocument();
        expect(screen.getByText(/Modified at/)).toBeInTheDocument();
    });

    it('underlines the label when the row is hovered', () => {
        renderRow();

        expect(screen.getByText('Mailer')).toHaveClass('group-hover/row:underline');
    });

    it('opens the workflow editor when the row is clicked', async () => {
        renderRow();

        await userEvent.click(screen.getByText('Mailer'));

        expect(handlers.onSelectWorkflow).toHaveBeenCalledWith('wf-1');
    });

    it('opens the workflow editor when the row outside the label is clicked', async () => {
        renderRow();

        await userEvent.click(screen.getByText(/Modified at/));

        expect(handlers.onSelectWorkflow).toHaveBeenCalledWith('wf-1');
    });

    describe('menu', () => {
        it('should render the shared workflow row menu with Edit, Duplicate, Export and Delete', async () => {
            await openMenu();

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
                'Edit',
                'Duplicate',
                'Export',
                'Delete',
            ]);
            expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
        });

        it('should edit the workflow when Edit is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

            expect(handlers.onEditWorkflow).toHaveBeenCalledWith(workflow);
            expect(handlers.onSelectWorkflow).not.toHaveBeenCalled();
        });

        it('should duplicate the workflow, refresh the projects and confirm it when Duplicate is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Duplicate'}));

            expect(mocks.duplicateMutate).toHaveBeenCalledWith({workflowUuid: 'wf-1'});

            act(() => {
                mocks.duplicateMutationOptions?.onSuccess?.();
            });

            expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationProjects']});
            expect(mocks.toast).toHaveBeenCalledWith('Workflow duplicated successfully.');
        });

        it('should download the workflow when Export is clicked', async () => {
            const originalLocation = window.location;

            Object.defineProperty(window, 'location', {configurable: true, value: {href: ''}});

            try {
                await openMenu();

                await userEvent.click(screen.getByRole('menuitem', {name: 'Export'}));

                expect(window.location.href).toBe('/api/automation/internal/workflows/draft-wf-1/export');
            } finally {
                Object.defineProperty(window, 'location', {configurable: true, value: originalLocation});
            }
        });

        it('does not offer an Executions action', async () => {
            await openMenu();

            expect(screen.queryByRole('menuitem', {name: 'Workflow Executions'})).not.toBeInTheDocument();
        });

        it('should delete the workflow when Delete is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

            expect(handlers.onDeleteWorkflow).toHaveBeenCalledWith('wf-1');
        });
    });
});
