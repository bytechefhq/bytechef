import WorkflowListRowDropdownMenu from '@/shared/components/workflow/WorkflowListRowDropdownMenu';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
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
