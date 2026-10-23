import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerConfigurationCode from '../McpServerConfigurationCode';

const SERVER_URL = 'http://127.0.0.1:5173/api/embedded/abc/mcp';

const renderConfigurationCode = (onRefresh = vi.fn()) => {
    const {unmount} = render(
        <TooltipProvider>
            <McpServerConfigurationCode codeSnippet={SERVER_URL} onRefresh={onRefresh} />
        </TooltipProvider>
    );

    return {onRefresh, unmount};
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.restoreAllMocks();
});

describe('McpServerConfigurationCode', () => {
    it('asks for confirmation instead of regenerating the URL right away', async () => {
        const {onRefresh} = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));

        expect(screen.getByText('Regenerate server URL?')).toBeInTheDocument();
        expect(onRefresh).not.toHaveBeenCalled();
    });

    it('regenerates the URL once the confirmation is accepted', async () => {
        const {onRefresh} = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));
        await userEvent.click(screen.getByRole('button', {name: 'Regenerate'}));

        expect(onRefresh).toHaveBeenCalledTimes(1);
        expect(screen.queryByText('Regenerate server URL?')).not.toBeInTheDocument();
    });

    it('keeps the URL when the confirmation is cancelled', async () => {
        const {onRefresh} = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));
        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onRefresh).not.toHaveBeenCalled();
        expect(screen.queryByText('Regenerate server URL?')).not.toBeInTheDocument();
    });

    it('clears the refresh reset timer on unmount', async () => {
        const {unmount} = renderConfigurationCode();

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate server URL'}));

        const setTimeoutSpy = vi.spyOn(globalThis, 'setTimeout');
        const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout');

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate'}));

        const refreshTimeoutIndex = setTimeoutSpy.mock.calls.findIndex((call) => call[1] === 300);

        expect(refreshTimeoutIndex).toBeGreaterThanOrEqual(0);

        const refreshTimeoutId = setTimeoutSpy.mock.results[refreshTimeoutIndex].value;

        unmount();

        expect(clearTimeoutSpy).toHaveBeenCalledWith(refreshTimeoutId);
    });

    it('shows the server URL', () => {
        renderConfigurationCode();

        expect(screen.getByText(SERVER_URL)).toBeInTheDocument();
    });
});
