import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {updateStreamingMock, useAiAgentStreamResponseMock} = vi.hoisted(() => ({
    updateStreamingMock: vi.fn(),
    useAiAgentStreamResponseMock: vi.fn(),
}));

vi.mock('./hooks/useAiAgentStreamResponse', () => ({
    default: useAiAgentStreamResponseMock,
}));

import AiAgentStreamResponseField from './AiAgentStreamResponseField';

const mockHook = (isStreaming: boolean, isStreamingSupported: boolean) => {
    useAiAgentStreamResponseMock.mockReturnValue({
        isStreaming,
        isStreamingSupported,
        updateStreaming: updateStreamingMock,
    });
};

describe('AiAgentStreamResponseField', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders nothing for an action that does not support streaming', () => {
        mockHook(false, false);

        const {container} = render(<AiAgentStreamResponseField />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders the title as a standalone heading, with the switch on its own row below', () => {
        mockHook(false, true);

        render(<AiAgentStreamResponseField />);

        const heading = screen.getByRole('heading', {name: 'Stream response'});
        const switchElement = screen.getByRole('switch', {name: /stream response/i});

        expect(heading).toBeInTheDocument();
        expect(heading).not.toContainElement(switchElement);
        expect(screen.getByText(/token by token/i)).toBeInTheDocument();
    });

    it('renders an unchecked switch for the chat action', () => {
        mockHook(false, true);

        render(<AiAgentStreamResponseField />);

        expect(screen.getByRole('switch', {name: /stream response/i})).not.toBeChecked();
    });

    it('renders a checked switch for the streamChat action', () => {
        mockHook(true, true);

        render(<AiAgentStreamResponseField />);

        expect(screen.getByRole('switch', {name: /stream response/i})).toBeChecked();
    });

    it('turns streaming on when the switch is toggled', async () => {
        mockHook(false, true);

        render(<AiAgentStreamResponseField />);

        await userEvent.click(screen.getByRole('switch', {name: /stream response/i}));

        expect(updateStreamingMock).toHaveBeenCalledWith(true);
    });
});
