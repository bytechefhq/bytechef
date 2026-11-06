import {TooltipProvider} from '@/components/ui/tooltip';
import OpenIntegrationWorkflowButton from '@/ee/pages/embedded/shared/OpenIntegrationWorkflowButton';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const {openIntegrationWorkflowMock} = vi.hoisted(() => ({openIntegrationWorkflowMock: vi.fn()}));

vi.mock('@/ee/pages/embedded/shared/useOpenInEmbeddedEditor', () => ({
    default: () => ({openIntegrationWorkflow: openIntegrationWorkflowMock}),
}));

describe('OpenIntegrationWorkflowButton', () => {
    it('opens the configured workflow in its integration', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <OpenIntegrationWorkflowButton integrationId={5} workflowUuid="workflow-uuid" />
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Open in integration'}));

        expect(openIntegrationWorkflowMock).toHaveBeenCalledWith(5, 'workflow-uuid');
    });
});
