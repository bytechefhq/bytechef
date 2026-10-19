import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubRetryBanner from '../AiHubRetryBanner';

const hoisted = vi.hoisted(() => ({append: vi.fn()}));

vi.mock('@assistant-ui/react', () => ({
    useAui: () => ({thread: {append: hoisted.append}}),
}));

const UNRECOVERED_FAILURE = {
    errorMessage: 'Component slack/v9 does not exist',
    lastUserMessage: 'Build a Slack workflow',
    toolName: 'buildWorkflow',
};

describe('AiHubRetryBanner', () => {
    beforeEach(() => {
        aiChatRetryableErrorStore.setState({currentError: undefined});

        hoisted.append.mockReset();
    });

    it('renders nothing while no tool failure is waiting', () => {
        const {container} = render(<AiHubRetryBanner />);

        expect(container).toBeEmptyDOMElement();
    });

    it('shows the unrecovered tool failure with Retry and Dismiss', () => {
        aiChatRetryableErrorStore.getState().setError(UNRECOVERED_FAILURE);

        render(<AiHubRetryBanner />);

        expect(screen.getByRole('alert')).toHaveTextContent('buildWorkflow failed: Component slack/v9 does not exist');
        expect(screen.getByRole('button', {name: 'Retry'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Dismiss'})).toBeInTheDocument();
    });

    it('Retry clears the failure and sends the last user message again', async () => {
        aiChatRetryableErrorStore.getState().setError(UNRECOVERED_FAILURE);

        render(<AiHubRetryBanner />);

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(hoisted.append).toHaveBeenCalledExactlyOnceWith({
            content: [{text: 'Build a Slack workflow', type: 'text'}],
            role: 'user',
        });
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('Dismiss clears the failure without sending anything', async () => {
        aiChatRetryableErrorStore.getState().setError(UNRECOVERED_FAILURE);

        render(<AiHubRetryBanner />);

        await userEvent.click(screen.getByRole('button', {name: 'Dismiss'}));

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        expect(hoisted.append).not.toHaveBeenCalled();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('offers no Retry when there is no user message to send again', () => {
        aiChatRetryableErrorStore.getState().setError({...UNRECOVERED_FAILURE, lastUserMessage: ''});

        render(<AiHubRetryBanner />);

        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Retry'})).not.toBeInTheDocument();
    });
});
