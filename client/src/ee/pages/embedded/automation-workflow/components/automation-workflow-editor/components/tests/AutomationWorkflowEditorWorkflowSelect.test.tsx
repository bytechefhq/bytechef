import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorWorkflowSelect from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowSelect';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {beforeEach, expect, it, vi} from 'vitest';

type WorkflowsPropType = ComponentProps<typeof AutomationWorkflowEditorWorkflowSelect>['workflows'];

const mockOnValueChange = vi.fn();

const mockWorkflows = [
    {label: 'Workflow 1', workflowUuid: 'uuid-1'},
    {label: '', workflowUuid: 'uuid-2'},
] as never as WorkflowsPropType;

const renderWorkflowSelect = (currentWorkflowId = 'uuid-1') => {
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorWorkflowSelect
                currentWorkflowId={currentWorkflowId}
                onValueChange={mockOnValueChange}
                workflows={mockWorkflows}
            />
        </TooltipProvider>
    );
};

beforeEach(() => {
    vi.clearAllMocks();
});

it('should render the label of the current workflow on the closed trigger', () => {
    renderWorkflowSelect();

    expect(screen.getByLabelText('Select workflow')).toBeInTheDocument();

    expect(screen.getByText('Workflow 1')).toBeInTheDocument();
});

it('should fall back to the workflow uuid when the current workflow has no label', () => {
    renderWorkflowSelect('uuid-2');

    expect(screen.getByText('uuid-2')).toBeInTheDocument();
});

it('should show the placeholder when no workflow matches the current id', () => {
    renderWorkflowSelect('uuid-missing');

    expect(screen.getByText('Select a workflow')).toBeInTheDocument();
});

it('should mark the current workflow as the checked menu item', async () => {
    renderWorkflowSelect();

    await userEvent.click(screen.getByLabelText('Select workflow'));

    const menuItems = screen.getAllByRole('menuitemradio');

    expect(menuItems[0]).toHaveAttribute('aria-checked', 'true');

    expect(menuItems[1]).toHaveAttribute('aria-checked', 'false');
});

it('should call the onValueChange function with the workflow uuid on click', async () => {
    renderWorkflowSelect();

    await userEvent.click(screen.getByLabelText('Select workflow'));

    await userEvent.click(screen.getByText('uuid-2'));

    expect(mockOnValueChange).toHaveBeenCalledWith('uuid-2');
});
