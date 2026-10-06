import {Thread} from '@/components/assistant-ui/thread';
import {render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {AssistantRuntimeProvider, type ExternalStoreAdapter, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const MESSAGES: ThreadMessageLike[] = [
    {content: 'Hello', role: 'user'},
    {content: 'Hi there', role: 'assistant'},
];

type RuntimeCallbacksType = Pick<ExternalStoreAdapter<ThreadMessageLike>, 'onEdit' | 'onReload'>;

const TestRuntimeProvider = ({callbacks}: {callbacks: RuntimeCallbacksType}) => {
    const runtime = useExternalStoreRuntime<ThreadMessageLike>({
        convertMessage: (message) => message,
        isRunning: false,
        messages: MESSAGES,
        onNew: vi.fn(),
        ...callbacks,
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
    it('should hide Edit and Refresh when the runtime supports neither editing nor reloading', async () => {
        render(<TestRuntimeProvider callbacks={{}} />);

        expect(await screen.findByText('Hi there')).toBeInTheDocument();

        await userEvent.hover(screen.getByText('Hello'));

        expect(screen.queryByRole('button', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Refresh'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Copy'})).toBeInTheDocument();
    });

    it('should show Edit and Refresh when the runtime supports editing and reloading', async () => {
        render(<TestRuntimeProvider callbacks={{onEdit: vi.fn(), onReload: vi.fn()}} />);

        expect(await screen.findByText('Hi there')).toBeInTheDocument();

        await userEvent.hover(screen.getByText('Hello'));

        expect(screen.getByRole('button', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Refresh'})).toBeInTheDocument();
    });
});
