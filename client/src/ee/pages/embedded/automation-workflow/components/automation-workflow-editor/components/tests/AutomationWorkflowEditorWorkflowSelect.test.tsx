import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorWorkflowSelect from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowSelect';
import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {beforeEach, expect, it, vi} from 'vitest';

type WorkflowsPropType = ComponentProps<typeof AutomationWorkflowEditorWorkflowSelect>['workflows'];

const mockOnValueChange = vi.fn();

const mockWorkflows = [
    {label: 'Workflow 1', workflowUuid: 'uuid-1'},
    {label: '', workflowUuid: 'uuid-2'},
] as never as WorkflowsPropType;

const renderWorkflowSelect = ({
    currentWorkflowId = 'uuid-1',
    workflows = mockWorkflows,
}: {currentWorkflowId?: string; workflows?: WorkflowsPropType} = {}) => {
    render(
        <TooltipProvider>
            <AutomationWorkflowEditorWorkflowSelect
                currentWorkflowId={currentWorkflowId}
                onValueChange={mockOnValueChange}
                workflows={workflows}
            />
        </TooltipProvider>
    );
};

const getMenuItemRadio = (label: string) =>
    within(screen.getByRole('menu')).getByText(label).closest('[role="menuitemradio"]');

beforeEach(() => {
    vi.clearAllMocks();
});

it('should render the label of the current workflow on the closed trigger', () => {
    renderWorkflowSelect();

    expect(screen.getByLabelText('Select workflow')).toBeInTheDocument();

    expect(screen.getByText('Workflow 1')).toBeInTheDocument();
});

it('should fall back to the workflow uuid when the current workflow has no label', () => {
    renderWorkflowSelect({currentWorkflowId: 'uuid-2'});

    expect(screen.getByText('uuid-2')).toBeInTheDocument();
});

it('should show a skeleton instead of a label when no workflow matches the current id', () => {
    renderWorkflowSelect({currentWorkflowId: 'uuid-missing'});

    expect(screen.getByLabelText('Select workflow')).toHaveTextContent('');
});

it('should list the project workflows under a Workflows group once opened', async () => {
    renderWorkflowSelect();

    await userEvent.click(screen.getByLabelText('Select workflow'));

    expect(screen.getByText('Workflows')).toBeInTheDocument();
    expect(within(screen.getByRole('menu')).getByText('uuid-2')).toBeInTheDocument();
});

it('should omit the Workflows group header when the project has no workflows', async () => {
    renderWorkflowSelect({workflows: []});

    await userEvent.click(screen.getByLabelText('Select workflow'));

    expect(screen.queryByText('Workflows')).not.toBeInTheDocument();
});

it('should highlight the current workflow as the checked menu item', async () => {
    renderWorkflowSelect();

    await userEvent.click(screen.getByLabelText('Select workflow'));

    expect(getMenuItemRadio('Workflow 1')).toHaveAttribute('aria-checked', 'true');
    expect(getMenuItemRadio('Workflow 1')).toHaveClass('data-[state=checked]:bg-surface-brand-secondary');

    expect(getMenuItemRadio('uuid-2')).toHaveAttribute('aria-checked', 'false');
});

it('should call the onValueChange function with the workflow uuid on click', async () => {
    renderWorkflowSelect();

    await userEvent.click(screen.getByLabelText('Select workflow'));

    await userEvent.click(screen.getByText('uuid-2'));

    expect(mockOnValueChange).toHaveBeenCalledWith('uuid-2');
});

it('should show the full label of a long current workflow in a tooltip', async () => {
    const longLabel = 'A workflow label that is long enough to be truncated';

    renderWorkflowSelect({
        workflows: [{label: longLabel, workflowUuid: 'uuid-1'}] as never as WorkflowsPropType,
    });

    await userEvent.hover(screen.getByLabelText('Select workflow'));

    expect(await screen.findByRole('tooltip')).toHaveTextContent(longLabel);
});

it('should title long workflow labels in the menu with their full text', async () => {
    const longLabel = 'A workflow label that is long enough to overflow the content-sized menu width';

    renderWorkflowSelect({
        workflows: [...mockWorkflows, {label: longLabel, workflowUuid: 'uuid-3'}] as never as WorkflowsPropType,
    });

    await userEvent.click(screen.getByLabelText('Select workflow'));

    expect(getMenuItemRadio(longLabel)).toHaveAttribute('title', longLabel);
    expect(getMenuItemRadio('Workflow 1')).not.toHaveAttribute('title');
});
