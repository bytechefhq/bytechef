import {TooltipProvider} from '@/components/ui/tooltip';
import OpenAutomationWorkflowButton from '@/ee/pages/embedded/shared/OpenAutomationWorkflowButton';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const {openAutomationWorkflowMock} = vi.hoisted(() => ({openAutomationWorkflowMock: vi.fn()}));

vi.mock('@/ee/pages/embedded/shared/useOpenInEmbeddedEditor', () => ({
    default: () => ({openAutomationWorkflow: openAutomationWorkflowMock}),
}));

describe('OpenAutomationWorkflowButton', () => {
    it('opens the workflow in the automation editor', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <OpenAutomationWorkflowButton label="Open in project" workflowUuid="workflow-uuid" />
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Open in project'}));

        expect(openAutomationWorkflowMock).toHaveBeenCalledWith('workflow-uuid');
    });
});
