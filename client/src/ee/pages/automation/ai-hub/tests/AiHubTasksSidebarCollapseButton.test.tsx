import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it} from 'vitest';

import AiHubTasksSidebarCollapseButton from '../AiHubTasksSidebarCollapseButton';

describe('AiHubTasksSidebarCollapseButton', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({tasksSidebarCollapsed: false});
    });

    it('renders a control to collapse the tasks sidebar', () => {
        render(
            <TooltipProvider>
                <AiHubTasksSidebarCollapseButton />
            </TooltipProvider>
        );

        expect(screen.getByRole('button', {name: 'Collapse tasks sidebar'})).toBeInTheDocument();
    });

    it('collapses the sidebar by setting tasksSidebarCollapsed when clicked', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <AiHubTasksSidebarCollapseButton />
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Collapse tasks sidebar'}));

        expect(aiHubTabsStore.getState().tasksSidebarCollapsed).toBe(true);
    });
});
