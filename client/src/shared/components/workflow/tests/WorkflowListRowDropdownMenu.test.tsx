import WorkflowListRowDropdownMenu from '@/shared/components/workflow/WorkflowListRowDropdownMenu';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    copyToClipboard: vi.fn(),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, hoisted.copyToClipboard],
}));

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

it('names the menu trigger after the workflow', async () => {
    const onEditClick = vi.fn();

    render(
        <WorkflowListRowDropdownMenu
            exportUrl="/export"
            onDeleteClick={vi.fn()}
            onEditClick={onEditClick}
            workflowLabel="Workflow One"
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

    expect(onEditClick).toHaveBeenCalled();
});

it('copies the workflow uuid', async () => {
    render(
        <WorkflowListRowDropdownMenu
            exportUrl="/export"
            onDeleteClick={vi.fn()}
            onEditClick={vi.fn()}
            workflowLabel="Workflow One"
            workflowUuid="workflow-uuid-1"
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Copy Workflow UUID'}));

    expect(hoisted.copyToClipboard).toHaveBeenCalledWith('workflow-uuid-1');
});

it('offers no Copy Workflow UUID without a workflow uuid', async () => {
    render(
        <WorkflowListRowDropdownMenu
            exportUrl="/export"
            onDeleteClick={vi.fn()}
            onEditClick={vi.fn()}
            workflowLabel="Workflow One"
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'Workflow actions for Workflow One'}));

    expect(screen.queryByRole('menuitem', {name: 'Copy Workflow UUID'})).not.toBeInTheDocument();
});
