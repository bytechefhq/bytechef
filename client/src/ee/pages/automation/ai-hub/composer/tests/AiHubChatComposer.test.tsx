import {TooltipProvider} from '@/components/ui/tooltip';
import {
    type ReferencedResourceI,
    aiHubComposerStore,
} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * Focused coverage for AiHubChatComposer's presentational layer — the referenced-resource chip row,
 * each chip's remove (X) control, and the empty-state behaviour. The component previously had no test
 * at all: a test-suite revival deleted the old (skipped) AiHubComposer chip/remove/empty-state cases
 * on the basis the behaviour "moved to AiHubChatComposer" — but AiHubChatComposer was never given a
 * test. This file closes that gap.
 *
 * The upload pipeline is NOT exercised here (it is already covered by useAiHubAttachmentUpload.test.ts);
 * the upload hook is mocked to return no uploads so only the chip layer is under test. The assistant-ui
 * runtime primitives, voice input, GraphQL mutations, and sibling composer subtrees are stubbed so the
 * component renders in isolation.
 */

// useAiHubAttachmentUpload owns the upload pipeline (covered by its own test). Stub it so the composer
// renders with no uploads and the chip row reflects only referencedResources.
vi.mock('@/ee/pages/automation/ai-hub/composer/hooks/useAiHubAttachmentUpload', () => ({
    ALLOWED_MIME_TYPES: ['image/png'],
    useAiHubAttachmentUpload: () => ({
        dismiss: vi.fn(),
        retry: vi.fn(),
        upload: vi.fn(),
        uploads: [],
    }),
}));

// AiHubComposerDropZone is a sibling (covered by its own test) — render its children directly so the
// composer tree mounts without the drag-and-drop machinery.
vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubComposerDropZone', () => ({
    default: ({children}: {children: ReactNode}) => <div data-testid="drop-zone">{children}</div>,
}));

// AiHubComposer (the ResourcePickerMenu trigger) and ChatToolChips are sibling subtrees, not under
// test here — stub them to keep the render lightweight.
vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubComposer', () => ({
    default: () => <div data-testid="ai-hub-composer" />,
}));

vi.mock('@/ee/pages/automation/ai-hub/tools/ChatToolChips', () => ({
    default: () => <div data-testid="chat-tool-chips" />,
}));

// The assistant-ui runtime primitives require an AssistantRuntimeProvider context. Stub the pieces the
// composer uses so it mounts standalone: Root/Input render plain elements, Send/If/Dictate render their
// children, AuiIf renders nothing (the dictation buttons are not under test), and the runtime hooks
// return inert objects.
vi.mock('@assistant-ui/react', () => ({
    AuiIf: () => null,
    ComposerPrimitive: {
        Dictate: ({children}: {children: ReactNode}) => <>{children}</>,
        Input: (props: Record<string, unknown>) => <textarea {...props} />,
        Root: ({children}: {children: ReactNode}) => <div data-testid="composer-root">{children}</div>,
        Send: ({children}: {children: ReactNode}) => <>{children}</>,
        StopDictation: ({children}: {children: ReactNode}) => <>{children}</>,
    },
    ThreadPrimitive: {
        If: ({children, running}: {children: ReactNode; running: boolean}) => (running ? null : <>{children}</>),
    },
    useAui: () => ({
        composer: {
            getState: () => ({text: ''}),
            send: vi.fn(),
            setText: vi.fn(),
        },
        subscribe: () => () => {},
        thread: {cancelRun: vi.fn()},
    }),
}));

// GraphQL cancel mutations are only invoked on a Stop click — stub them so the component mounts.
vi.mock('@/shared/middleware/graphql', () => ({
    useAiSkillsQuery: () => ({data: undefined}),
    useCancelAiHubRunMutation: () => ({mutate: vi.fn()}),
    useCancelWorkflowChatTurnMutation: () => ({mutate: vi.fn()}),
}));

// The active-chat lookup (currentChatId from the chats store ∩ the chats query) drives `isWorkflowChat`
// (gates the resource-attachment controls) and `isChannelBornChat` (gates the message input itself).
// Hoisted mutable refs let each test set the active chat's kind/aiAgentId/workflowExecutionId; all default
// to "no active chat" so existing chip tests see a standard (non-workflow) composer.
interface MockChatI {
    aiAgentId?: number | null;
    id: number;
    kind: string;
    workflowExecutionId?: string | null;
}

const {chatsQueryRef, currentChatIdRef} = vi.hoisted(() => ({
    chatsQueryRef: {current: undefined as MockChatI[] | undefined},
    currentChatIdRef: {current: undefined as number | undefined},
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatsQuery: () => ({data: chatsQueryRef.current}),
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore', () => ({
    useAiHubChatsStore: (selector: (state: {currentChatId: number | undefined}) => unknown) =>
        selector({currentChatId: currentChatIdRef.current}),
}));

// Workspace / environment stores are read via selectors; constant returns are enough here.
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 1),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 0),
}));

const dataTableResource: ReferencedResourceI = {id: 'dt-1', kind: 'dataTable', name: 'Customers'};
const knowledgeBaseResource: ReferencedResourceI = {id: 'kb-2', kind: 'knowledgeBase', name: 'Product Docs'};

const renderComposer = async () => {
    const {default: AiHubChatComposer} = await import('../AiHubChatComposer');

    return render(
        <TooltipProvider>
            <AiHubChatComposer />
        </TooltipProvider>
    );
};

beforeEach(() => {
    aiHubComposerStore.setState({referencedResources: [], resourcePickerOpen: false, selectedSkills: []});

    currentChatIdRef.current = undefined;
    chatsQueryRef.current = undefined;
});

describe('AiHubChatComposer skill chips', () => {
    it('renders a chip for each skill armed through the / menu and removes it on X', async () => {
        aiHubComposerStore.setState({selectedSkills: [{id: 'skill-1', name: 'email-digest'}]});

        await renderComposer();

        expect(screen.getByTestId('skill-chip')).toBeInTheDocument();
        expect(screen.getByText('email-digest')).toBeInTheDocument();

        fireEvent.click(screen.getByLabelText('Remove email-digest'));

        expect(aiHubComposerStore.getState().selectedSkills).toEqual([]);
        expect(screen.queryByText('email-digest')).not.toBeInTheDocument();
    });
});

describe('AiHubChatComposer referenced-resource chips', () => {
    it('renders a chip for each referenced resource in the composer store', async () => {
        aiHubComposerStore.setState({referencedResources: [dataTableResource, knowledgeBaseResource]});

        await renderComposer();

        expect(screen.getByTestId('reference-chips')).toBeInTheDocument();
        expect(screen.getByText('Customers')).toBeInTheDocument();
        expect(screen.getByText('Product Docs')).toBeInTheDocument();
    });

    it('removes a referenced resource from the store when its X control is clicked', async () => {
        aiHubComposerStore.setState({referencedResources: [dataTableResource, knowledgeBaseResource]});

        await renderComposer();

        fireEvent.click(screen.getByLabelText('Remove Customers'));

        const {referencedResources} = aiHubComposerStore.getState();

        expect(referencedResources).toEqual([knowledgeBaseResource]);
        expect(screen.queryByText('Customers')).not.toBeInTheDocument();
        expect(screen.getByText('Product Docs')).toBeInTheDocument();
    });

    it('omits the chip row entirely when there are no referenced resources or uploads', async () => {
        await renderComposer();

        expect(screen.queryByTestId('reference-chips')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Remove /})).not.toBeInTheDocument();
    });
});

describe('AiHubChatComposer workflow-chat attachment gating', () => {
    it('renders the resource picker and attach-file button for a standard chat', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        expect(screen.getByTestId('ai-hub-composer')).toBeInTheDocument();
        expect(screen.getByLabelText('Attach file')).toBeInTheDocument();
    });

    it('hides the resource picker and attach-file button for a workflow chat', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'WORKFLOW_CHAT'}];

        await renderComposer();

        expect(screen.queryByTestId('ai-hub-composer')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Attach file')).not.toBeInTheDocument();
    });
});

describe('AiHubChatComposer channel-born agent chat input gating', () => {
    // Channel-born AGENT_CHAT rows (aiAgentId stamped, workflowExecutionId null — see isChannelAgentChat)
    // must not offer a typeable input at all: the conversation is driven by its channel (Slack, a
    // schedule, …), not by AI Hub, so a message typed here has nowhere real to go. This is the fix for
    // the gap where isWorkflowChat alone hid only the attachment controls above, leaving the message
    // input itself enabled — this test fails if that gate is ever removed or narrowed back to
    // isWorkflowChat.
    it('replaces the message input with a read-only notice for a channel-born agent chat', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{aiAgentId: 9, id: 7, kind: 'AGENT_CHAT', workflowExecutionId: null}];

        await renderComposer();

        expect(screen.queryByLabelText('Message input')).not.toBeInTheDocument();
        expect(screen.getByTestId('channel-born-readonly-notice')).toBeInTheDocument();
        expect(screen.getByText(/happens on its channel/)).toBeInTheDocument();
    });

    it('still renders a typeable message input for a composer-created agent chat (aiAgentId null)', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{aiAgentId: null, id: 7, kind: 'AGENT_CHAT', workflowExecutionId: 'exec-1'}];

        await renderComposer();

        expect(screen.getByLabelText('Message input')).toBeInTheDocument();
        expect(screen.queryByTestId('channel-born-readonly-notice')).not.toBeInTheDocument();
    });

    it('still renders a typeable message input for a WORKFLOW_CHAT — typing is the entire point of it', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{aiAgentId: null, id: 7, kind: 'WORKFLOW_CHAT', workflowExecutionId: 'exec-1'}];

        await renderComposer();

        expect(screen.getByLabelText('Message input')).toBeInTheDocument();
        expect(screen.queryByTestId('channel-born-readonly-notice')).not.toBeInTheDocument();
    });

    it('still renders a typeable message input for a STANDARD chat', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        expect(screen.getByLabelText('Message input')).toBeInTheDocument();
        expect(screen.queryByTestId('channel-born-readonly-notice')).not.toBeInTheDocument();
    });
});

describe("AiHubChatComposer '@' resource-picker trigger", () => {
    // The '@' key is the composer's second way into the resource picker (the "+" button being the first).
    // It raises the picker by flipping a store flag AiHubComposer carries down — a keystroke in this
    // textarea cannot otherwise reach a popover owned by a sibling component.
    const pressAt = (caret: number, value = '') => {
        const textarea = screen.getByLabelText('Message input') as HTMLTextAreaElement;

        textarea.value = value;
        textarea.selectionStart = caret;
        textarea.selectionEnd = caret;

        return fireEvent.keyDown(textarea, {key: '@'});
    };

    it('opens the picker when @ is typed at the start of an empty composer', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        pressAt(0);

        expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(true);
    });

    it('opens the picker when @ follows a space mid-sentence', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        pressAt(5, 'look ');

        expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(true);
    });

    // Without the word-boundary rule, typing an email address would pop the picker open mid-word and
    // swallow the '@' — the address would come out as "supportbytechef.io".
    it('leaves @ as ordinary text mid-word, so an email address still types through', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        const defaultNotPrevented = pressAt(7, 'support');

        expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(false);
        // fireEvent returns false when preventDefault was called; the '@' must reach the textarea here.
        expect(defaultNotPrevented).toBe(true);
    });

    it('consumes the @ keystroke when it opens the picker', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        expect(pressAt(0)).toBe(false);
    });

    // Radix hands focus back to the popover trigger — the "+" button — on close. After a '@' the user was
    // mid-sentence, so the caret has to come back to the textarea or their next keystroke is swallowed by a
    // button. This is the half of the flow that makes '@' usable rather than merely functional.
    it('returns focus to the textarea when a picker raised by @ closes', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        const textarea = screen.getByLabelText('Message input') as HTMLTextAreaElement;

        pressAt(0);
        textarea.blur();

        act(() => {
            aiHubComposerStore.getState().setResourcePickerOpen(false);
        });

        await waitFor(() => {
            expect(textarea).toHaveFocus();
        });
    });

    // The "+" button must keep Radix's ordinary trigger-restore behaviour: nobody was typing, so stealing
    // the caret into the textarea would be the surprising move.
    it('leaves focus alone when the picker was opened without @', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{id: 7, kind: 'STANDARD'}];

        await renderComposer();

        const textarea = screen.getByLabelText('Message input') as HTMLTextAreaElement;

        act(() => {
            aiHubComposerStore.getState().setResourcePickerOpen(true);
        });

        textarea.blur();

        act(() => {
            aiHubComposerStore.getState().setResourcePickerOpen(false);
        });

        await waitFor(() => {
            expect(textarea).not.toHaveFocus();
        });
    });

    // A workflow chat forwards messages to a webhook rather than an agent, so it renders no picker at all
    // (see the attachment-gating suite above). '@' must not open one that isn't there.
    it('stays inert for a workflow chat, which has no resource picker', async () => {
        currentChatIdRef.current = 7;
        chatsQueryRef.current = [{aiAgentId: null, id: 7, kind: 'WORKFLOW_CHAT', workflowExecutionId: 'exec-1'}];

        await renderComposer();

        pressAt(0);

        expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(false);
    });
});
