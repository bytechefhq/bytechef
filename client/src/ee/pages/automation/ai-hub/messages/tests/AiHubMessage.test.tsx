import {AiHubMessageComponents} from '@/ee/pages/automation/ai-hub/messages/AiHubMessage';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

interface FakeMessageStateI {
    content: unknown[];
}

const {messageStateRef} = vi.hoisted(() => ({
    messageStateRef: {current: {content: []} as FakeMessageStateI},
}));

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubMessageContent', () => ({
    default: () => <div data-testid="message-content" />,
}));

vi.mock('@assistant-ui/react', () => ({
    ActionBarPrimitive: {
        Copy: ({children}: {children: ReactNode}) => <>{children}</>,
        Edit: ({children}: {children: ReactNode}) => <>{children}</>,
        Reload: ({children}: {children: ReactNode}) => <>{children}</>,
        Root: ({children}: {children: ReactNode}) => <div>{children}</div>,
    },
    MessagePrimitive: {
        If: ({children}: {children: ReactNode}) => <>{children}</>,
        Root: ({children}: {children: ReactNode}) => <>{children}</>,
    },
    useAuiState: (selector: (state: {message: FakeMessageStateI}) => unknown) =>
        selector({message: messageStateRef.current}),
}));

const {AssistantMessage, UserMessage} = AiHubMessageComponents;

beforeEach(() => {
    messageStateRef.current = {content: []};
});

describe('AiHubUserMessage', () => {
    it('renders the synthetic tool-approval message as a status line, not a bubble', () => {
        messageStateRef.current = {content: [{text: '[tool-approval #1234 approved and executed]', type: 'text'}]};

        render(<UserMessage />);

        expect(screen.getByTestId('tool-approval-status-line')).toHaveTextContent(
            'tool-approval #1234 approved and executed'
        );
        expect(screen.queryByTestId('message-content')).not.toBeInTheDocument();
    });

    it('renders the normal bubble for an ordinary user message, with no author label', () => {
        messageStateRef.current = {content: [{text: 'hello there', type: 'text'}]};

        render(<UserMessage />);

        expect(screen.getByTestId('message-content')).toBeInTheDocument();
        expect(screen.queryByTestId('tool-approval-status-line')).not.toBeInTheDocument();
        expect(screen.queryByTestId('message-author')).not.toBeInTheDocument();
    });
});

describe('AiHubAssistantMessage', () => {
    it('renders the content and the copy / refresh action bar', () => {
        render(<AssistantMessage />);

        expect(screen.getByTestId('message-content')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Copy'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Refresh'})).toBeInTheDocument();
    });
});
