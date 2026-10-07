import {Thread} from '@/components/assistant-ui/thread';
import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const IMAGE_SRC = 'data:image/png;base64,iVBORw0KGgo=';

const MESSAGES: ThreadMessageLike[] = [
    {
        attachments: [
            {
                content: [{image: IMAGE_SRC, type: 'image'}],
                id: 'attachment-1',
                name: 'diagram.png',
                status: {type: 'complete'},
                type: 'image',
            },
        ],
        content: 'See attached',
        role: 'user',
    },
];

const TestRuntimeProvider = () => {
    const runtime = useExternalStoreRuntime<ThreadMessageLike>({
        convertMessage: (message) => message,
        isRunning: false,
        messages: MESSAGES,
        onNew: vi.fn(),
    });

    return (
        <AssistantRuntimeProvider runtime={runtime}>
            <TooltipProvider>
                <Thread />
            </TooltipProvider>
        </AssistantRuntimeProvider>
    );
};

beforeEach(() => {
    windowResizeObserver();
});

describe('AttachmentPreviewDialog', () => {
    it('should open the image preview on the design-system dialog surface', async () => {
        render(<TestRuntimeProvider />);

        await userEvent.click(await screen.findByRole('button', {name: 'Image attachment'}));

        const dialog = await screen.findByRole('dialog', {name: 'Image Attachment Preview'});

        expect(dialog).toHaveClass('sm:w-[800px]');
        expect(dialog.querySelector('[data-slot="dialog-main"]')).toBeInTheDocument();
        expect(screen.getByAltText('Attachment preview')).toHaveAttribute('src', IMAGE_SRC);
    });

    it('should close the image preview on Escape', async () => {
        render(<TestRuntimeProvider />);

        await userEvent.click(await screen.findByRole('button', {name: 'Image attachment'}));

        expect(await screen.findByRole('dialog')).toBeInTheDocument();

        await userEvent.keyboard('{Escape}');

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
