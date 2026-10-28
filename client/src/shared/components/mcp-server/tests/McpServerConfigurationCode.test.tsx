import {TooltipProvider} from '@/components/ui/tooltip';
import {act, fireEvent, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerConfigurationCode from '../McpServerConfigurationCode';

const SERVER_URL = 'http://127.0.0.1:5173/api/embedded/abc/mcp';

const renderConfigurationCode = (onRefresh = vi.fn()) => {
    render(
        <TooltipProvider>
            <McpServerConfigurationCode codeSnippet={SERVER_URL} onRefresh={onRefresh} />
        </TooltipProvider>
    );

    return onRefresh;
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
    vi.useRealTimers();
});

describe('McpServerConfigurationCode', () => {
    it('asks for confirmation instead of regenerating the URL right away', async () => {
        const onRefresh = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));

        expect(screen.getByText('Regenerate server URL?')).toBeInTheDocument();
        expect(onRefresh).not.toHaveBeenCalled();
    });

    it('regenerates the URL once the confirmation is accepted', async () => {
        const onRefresh = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));
        await userEvent.click(screen.getByRole('button', {name: 'Regenerate'}));

        expect(onRefresh).toHaveBeenCalledTimes(1);
        expect(screen.queryByText('Regenerate server URL?')).not.toBeInTheDocument();
    });

    it('keeps the URL when the confirmation is cancelled', async () => {
        const onRefresh = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));
        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onRefresh).not.toHaveBeenCalled();
        expect(screen.queryByText('Regenerate server URL?')).not.toBeInTheDocument();
    });

    it('shows the server URL', () => {
        renderConfigurationCode();

        expect(screen.getByText(SERVER_URL)).toBeInTheDocument();
    });

    describe('copy', () => {
        const writeText = vi.fn();

        beforeEach(() => {
            Object.defineProperty(navigator, 'clipboard', {configurable: true, value: {writeText}});

            vi.useFakeTimers();
        });

        it('copies the server URL and shows the copied state for two seconds', () => {
            const {container} = render(
                <TooltipProvider>
                    <McpServerConfigurationCode codeSnippet={SERVER_URL} onRefresh={vi.fn()} />
                </TooltipProvider>
            );

            fireEvent.click(screen.getByRole('button', {name: 'Copy server URL'}));

            expect(writeText).toHaveBeenCalledWith(SERVER_URL);
            expect(container.querySelector('.lucide-check')).toBeInTheDocument();

            act(() => {
                vi.advanceTimersByTime(2000);
            });

            expect(container.querySelector('.lucide-check')).not.toBeInTheDocument();
        });

        it('clears its pending timers when it unmounts', () => {
            const {unmount} = render(
                <TooltipProvider>
                    <McpServerConfigurationCode codeSnippet={SERVER_URL} onRefresh={vi.fn()} />
                </TooltipProvider>
            );

            fireEvent.click(screen.getByRole('button', {name: 'Copy server URL'}));

            expect(vi.getTimerCount()).toBeGreaterThan(0);

            unmount();

            expect(vi.getTimerCount()).toBe(0);
        });
    });
});
