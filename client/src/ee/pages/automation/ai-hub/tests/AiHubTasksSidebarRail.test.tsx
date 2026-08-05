import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it} from 'vitest';

import AiHubTasksSidebarRail from '../AiHubTasksSidebarRail';

describe('AiHubTasksSidebarRail', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({tasksSidebarCollapsed: true});
    });

    it('renders a single control to show the tasks sidebar', () => {
        render(
            <TooltipProvider>
                <AiHubTasksSidebarRail />
            </TooltipProvider>
        );

        expect(screen.getByRole('button', {name: 'Show tasks sidebar'})).toBeInTheDocument();
    });

    it('expands the sidebar by clearing tasksSidebarCollapsed when clicked', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <AiHubTasksSidebarRail />
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Show tasks sidebar'}));

        expect(aiHubTabsStore.getState().tasksSidebarCollapsed).toBe(false);
    });
});
