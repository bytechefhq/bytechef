import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it} from 'vitest';

import AiHubChatsSidebarPinButton from '../AiHubChatsSidebarPinButton';

describe('AiHubChatsSidebarPinButton', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({chatsSidebarCollapsed: true, chatsSidebarPeeking: true});
    });

    it('renders a control to pin the peeked chats sidebar open', () => {
        render(
            <TooltipProvider>
                <AiHubChatsSidebarPinButton />
            </TooltipProvider>
        );

        expect(screen.getByRole('button', {name: 'Pin chats sidebar open'})).toBeInTheDocument();
    });

    it('docks the sidebar open and ends the peek when clicked', async () => {
        const user = userEvent.setup();

        render(
            <TooltipProvider>
                <AiHubChatsSidebarPinButton />
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Pin chats sidebar open'}));

        expect(aiHubTabsStore.getState().chatsSidebarCollapsed).toBe(false);
        expect(aiHubTabsStore.getState().chatsSidebarPeeking).toBe(false);
    });
});
