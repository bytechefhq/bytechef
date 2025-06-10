import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowActionsButton from '@/pages/automation/project/components/project-header/components/WorkflowActionsButton';
import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

screen.debug();

const mockOnRunClick = vi.fn();

const mockOnStopClick = vi.fn();

const renderWorkflowActionsButton = (chatTrigger: boolean, workflowIsRunning?: boolean, runDisabled?: boolean) => {
    render(
        <TooltipProvider>
            <WorkflowActionsButton
                chatTrigger={chatTrigger}
                onRunClick={mockOnRunClick}
                onStopClick={mockOnStopClick}
                runDisabled={runDisabled ?? false}
                workflowIsRunning={workflowIsRunning ?? false}
            />
        </TooltipProvider>
    );
};

it('should show the workflow RUN button when chat trigger is false', () => {
    renderWorkflowActionsButton(false);

    expect(screen.getByText('Test')).toBeInTheDocument();

    expect(screen.queryByText('Chat')).not.toBeInTheDocument();
});

it('should show the workflow STOP button when workflow is running', () => {
    renderWorkflowActionsButton(false, true);

    expect(screen.getByText('Stop')).toBeInTheDocument();

    expect(screen.queryByText('Test')).not.toBeInTheDocument();
    expect(screen.queryByText('Chat')).not.toBeInTheDocument();
});

it('should show the CHAT button instead of RUN when chat trigger is true', () => {
    renderWorkflowActionsButton(true);

    expect(screen.getByText('Chat')).toBeInTheDocument();

    expect(screen.queryByText('Test')).not.toBeInTheDocument();
});

it('should disable the button when runDisabled is true', () => {
    renderWorkflowActionsButton(false, false, true);

    expect(screen.getByText('Test')).toBeDisabled();
});

it('should call onRunClick when run button is clicked', async () => {
    renderWorkflowActionsButton(false, false, false);

    fireEvent.click(screen.getByText('Test'));

    await waitFor(() => expect(mockOnRunClick).toHaveBeenCalled());
});

it('should call onStopClick when stop button is clicked', async () => {
    renderWorkflowActionsButton(false, true, false);

    fireEvent.click(screen.getByText('Stop'));

    await waitFor(() => expect(mockOnStopClick).toHaveBeenCalled());
});
