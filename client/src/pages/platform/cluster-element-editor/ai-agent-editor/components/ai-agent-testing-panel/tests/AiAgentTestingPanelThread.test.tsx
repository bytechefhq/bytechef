import {render, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {Thread} from '../AiAgentTestingPanelThread';

const TestRuntimeProvider = ({messages}: {messages: ThreadMessageLike[]}) => {
    const runtime = useExternalStoreRuntime<ThreadMessageLike>({
        convertMessage: (message) => message,
        isRunning: false,
        messages,
        onNew: vi.fn(),
    });

    return (
        <AssistantRuntimeProvider runtime={runtime}>
            <Thread />
        </AssistantRuntimeProvider>
    );
};

beforeEach(() => {
    windowResizeObserver();
});

describe('Thread', () => {
    it('lists the suggested prompts beneath the composer before the conversation starts', async () => {
        const {container} = render(<TestRuntimeProvider messages={[]} />);

        const suggestion = await screen.findByRole('button', {name: 'What can you help me with?'});
        const composer = container.querySelector('.aui-composer-root');

        expect(composer).not.toBeNull();
        expect(composer!.compareDocumentPosition(suggestion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('centers the composer under the welcome message before the conversation starts', async () => {
        const {container} = render(<TestRuntimeProvider messages={[]} />);

        await screen.findByText('Test your agent');

        const welcome = container.querySelector('.aui-thread-welcome-root');
        const composerWrapper = container.querySelector('.aui-composer-wrapper');
        const spacers = container.querySelectorAll('.aui-thread-empty-spacer');

        expect(welcome!.compareDocumentPosition(composerWrapper!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(spacers).toHaveLength(2);
        expect(spacers[0].compareDocumentPosition(welcome!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(composerWrapper!.compareDocumentPosition(spacers[1]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('keeps the composer at the bottom once the conversation has started', async () => {
        const {container} = render(
            <TestRuntimeProvider
                messages={[
                    {content: 'Hello', role: 'user'},
                    {content: 'Hi there', role: 'assistant'},
                ]}
            />
        );

        expect(await screen.findByText('Hi there')).toBeInTheDocument();
        expect(container.querySelectorAll('.aui-thread-empty-spacer')).toHaveLength(0);
    });

    it('drops the suggested prompts once the conversation has started', async () => {
        render(
            <TestRuntimeProvider
                messages={[
                    {content: 'Hello', role: 'user'},
                    {content: 'Hi there', role: 'assistant'},
                ]}
            />
        );

        expect(await screen.findByText('Hi there')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'What can you help me with?'})).not.toBeInTheDocument();
    });
});
