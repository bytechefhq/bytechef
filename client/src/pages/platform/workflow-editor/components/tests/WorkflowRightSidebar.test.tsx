import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowRightSidebar from '@/pages/platform/workflow-editor/components/WorkflowRightSidebar';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (flag: string) => flag === 'ff-1570',
}));

const onWorkflowInputsClick = vi.fn();

const renderSidebar = (copilotOnly: boolean) =>
    render(
        <TooltipProvider>
            <WorkflowRightSidebar
                copilotOnly={copilotOnly}
                copilotPanelOpen={false}
                onComponentsAndFlowControlsClick={vi.fn()}
                onCopilotClick={vi.fn()}
                onWorkflowCodeEditorClick={vi.fn()}
                onWorkflowInputsClick={onWorkflowInputsClick}
                onWorkflowOutputsClick={vi.fn()}
                rightSidebarOpen={false}
            />
        </TooltipProvider>
    );

describe('WorkflowRightSidebar', () => {
    it('offers the full rail for a visually built workflow', () => {
        renderSidebar(false);

        expect(screen.getByLabelText('Components & Flow Controls')).toBeInTheDocument();
        expect(screen.getByLabelText('Workflow Inputs')).toBeInTheDocument();
        expect(screen.getByLabelText('Copilot')).toBeInTheDocument();
    });

    it('keeps Workflow Inputs for a code workflow, whose inputs are still worth reading', async () => {
        renderSidebar(true);

        // The canvas-only entries go: a code workflow has no node palette, no outputs panel, no code sheet.
        expect(screen.queryByLabelText('Components & Flow Controls')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Workflow Code Editor')).not.toBeInTheDocument();

        // Inputs and Copilot stay, in that order — inputs sits above Copilot in the rail.
        const buttons = screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'));

        expect(buttons).toEqual(['Workflow Inputs', 'Copilot']);

        await userEvent.click(screen.getByLabelText('Workflow Inputs'));

        expect(onWorkflowInputsClick).toHaveBeenCalledOnce();
    });
});
