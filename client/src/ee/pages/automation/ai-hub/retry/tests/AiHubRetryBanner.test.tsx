import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockAppend = vi.fn();

vi.mock('@assistant-ui/react', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@assistant-ui/react')>();

    return {
        ...actual,
        useAui: () => ({thread: {append: mockAppend}}),
    };
});

const renderBanner = async () => {
    const {default: AiHubRetryBanner} = await import('../AiHubRetryBanner');

    return render(<AiHubRetryBanner />);
};

describe('AiHubRetryBanner', () => {
    beforeEach(() => {
        aiChatRetryableErrorStore.setState({currentError: undefined});
        mockAppend.mockReset();
    });

    it('renders nothing when there is no current error', async () => {
        const {container} = await renderBanner();

        expect(container.firstChild).toBeNull();
    });

    it('renders the error banner when currentError is set', async () => {
        aiChatRetryableErrorStore.getState().setError({
            errorMessage: 'Report service unavailable',
            lastUserMessage: 'Create a report for Q1',
            toolName: 'createReport',
        });

        await renderBanner();

        expect(screen.getByText('"createReport" failed: Report service unavailable')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /retry/i})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /dismiss/i})).toBeInTheDocument();
    });

    it('Dismiss clears the error without calling append', async () => {
        aiChatRetryableErrorStore.getState().setError({
            errorMessage: 'Something broke',
            lastUserMessage: 'Do the thing',
            toolName: 'doThing',
        });

        await renderBanner();

        await userEvent.click(screen.getByRole('button', {name: /dismiss/i}));

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(mockAppend).not.toHaveBeenCalled();
    });

    it('Retry clears the error and calls threadRuntime.append with the last user message', async () => {
        aiChatRetryableErrorStore.getState().setError({
            errorMessage: 'Report service unavailable',
            lastUserMessage: 'Create a report for Q1',
            toolName: 'createReport',
        });

        await renderBanner();

        await userEvent.click(screen.getByRole('button', {name: /retry/i}));

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(mockAppend).toHaveBeenCalledOnce();
        expect(mockAppend).toHaveBeenCalledWith({
            content: [{text: 'Create a report for Q1', type: 'text'}],
            role: 'user',
        });
    });
});
