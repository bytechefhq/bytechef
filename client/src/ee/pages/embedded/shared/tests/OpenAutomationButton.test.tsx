import {TooltipProvider} from '@/components/ui/tooltip';
import OpenAutomationButton from '@/ee/pages/embedded/shared/OpenAutomationButton';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const {openAutomationMock} = vi.hoisted(() => ({openAutomationMock: vi.fn()}));

vi.mock('@/ee/pages/embedded/shared/useOpenInEmbeddedEditor', () => ({
    default: () => ({openAutomation: openAutomationMock}),
}));

describe('OpenAutomationButton', () => {
    it('opens the workflow in the automation editor', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <OpenAutomationButton label="Open in project" workflowUuid="workflow-uuid" />
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Open in project'}));

        expect(openAutomationMock).toHaveBeenCalledWith('workflow-uuid');
    });
});
