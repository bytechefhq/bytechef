import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {act, fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubChatsSidebarToggle, {PEEK_DELAY_MS} from '../AiHubChatsSidebarToggle';

const renderToggle = () =>
    render(
        <TooltipProvider>
            <AiHubChatsSidebarToggle />
        </TooltipProvider>
    );

describe('AiHubChatsSidebarToggle', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({chatsSidebarCollapsed: false, chatsSidebarPeeking: false});
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('hides the open sidebar when clicked', async () => {
        const user = userEvent.setup();

        renderToggle();

        await user.click(screen.getByRole('button', {name: 'Hide chats'}));

        expect(aiHubTabsStore.getState().chatsSidebarCollapsed).toBe(true);
        expect(screen.getByRole('button', {name: 'Show chats'})).toBeInTheDocument();
    });

    it('docks the hidden sidebar open when clicked, ending any peek', async () => {
        aiHubTabsStore.setState({chatsSidebarCollapsed: true, chatsSidebarPeeking: true});

        const user = userEvent.setup();

        renderToggle();

        await user.click(screen.getByRole('button', {name: 'Show chats'}));

        expect(aiHubTabsStore.getState().chatsSidebarCollapsed).toBe(false);
        expect(aiHubTabsStore.getState().chatsSidebarPeeking).toBe(false);
    });

    it('peeks the hidden sidebar after the pointer rests on the toggle', () => {
        vi.useFakeTimers();

        aiHubTabsStore.setState({chatsSidebarCollapsed: true});

        renderToggle();

        fireEvent.mouseEnter(screen.getByRole('button', {name: 'Show chats'}));

        // Not yet — hover intent delay.
        expect(aiHubTabsStore.getState().chatsSidebarPeeking).toBe(false);

        act(() => {
            vi.advanceTimersByTime(PEEK_DELAY_MS);
        });

        expect(aiHubTabsStore.getState().chatsSidebarPeeking).toBe(true);
    });

    it('cancels a pending peek when the pointer leaves before the delay elapses', () => {
        vi.useFakeTimers();

        aiHubTabsStore.setState({chatsSidebarCollapsed: true});

        renderToggle();

        const toggle = screen.getByRole('button', {name: 'Show chats'});

        fireEvent.mouseEnter(toggle);
        fireEvent.mouseLeave(toggle);

        act(() => {
            vi.advanceTimersByTime(PEEK_DELAY_MS * 2);
        });

        expect(aiHubTabsStore.getState().chatsSidebarPeeking).toBe(false);
    });

    it('does not peek while the sidebar is docked open', () => {
        vi.useFakeTimers();

        renderToggle();

        fireEvent.mouseEnter(screen.getByRole('button', {name: 'Hide chats'}));

        act(() => {
            vi.advanceTimersByTime(PEEK_DELAY_MS * 2);
        });

        expect(aiHubTabsStore.getState().chatsSidebarPeeking).toBe(false);
    });
});
