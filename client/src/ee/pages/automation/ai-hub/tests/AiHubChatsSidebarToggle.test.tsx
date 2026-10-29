import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it} from 'vitest';

import AiHubChatsSidebarToggle from '../AiHubChatsSidebarToggle';

const renderToggle = () =>
    render(
        <TooltipProvider>
            <AiHubChatsSidebarToggle />
        </TooltipProvider>
    );

describe('AiHubChatsSidebarToggle', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({chatsSidebarCollapsed: false});
    });

    it('hides the open sidebar when clicked', async () => {
        const user = userEvent.setup();

        renderToggle();

        await user.click(screen.getByRole('button', {name: 'Hide chats'}));

        expect(aiHubTabsStore.getState().chatsSidebarCollapsed).toBe(true);
        expect(screen.getByRole('button', {name: 'Show chats'})).toBeInTheDocument();
    });

    it('shows the hidden sidebar when clicked', async () => {
        aiHubTabsStore.setState({chatsSidebarCollapsed: true});

        const user = userEvent.setup();

        renderToggle();

        await user.click(screen.getByRole('button', {name: 'Show chats'}));

        expect(aiHubTabsStore.getState().chatsSidebarCollapsed).toBe(false);
    });
});
