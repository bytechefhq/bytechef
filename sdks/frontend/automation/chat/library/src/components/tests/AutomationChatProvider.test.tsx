import {AppendMessage, useExternalStoreRuntime} from '@assistant-ui/react';
import {act, render} from '@testing-library/react';
import {ReactNode, useEffect} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AutomationChatProvider} from '@/components/AutomationChatProvider';
import {ApprovalResolutionContextI, useApprovalResolution} from '@/components/approvalResolutionContext';
import type {UseSSEOptionsType} from '@/hooks/useSSE';
import {useChatStore} from '@/stores/useChatStore';

const {useSSEMock} = vi.hoisted(() => ({
    useSSEMock: vi.fn(),
}));

vi.mock('@/hooks/useSSE', () => ({
    useSSE: useSSEMock,
}));

vi.mock('@assistant-ui/react', () => ({
    AssistantRuntimeProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
    CompositeAttachmentAdapter: vi.fn(),
    SimpleImageAttachmentAdapter: vi.fn(),
    SimpleTextAttachmentAdapter: vi.fn(),
    Suggestions: vi.fn(),
    useAui: vi.fn(() => ({})),
    useExternalStoreRuntime: vi.fn(() => ({})),
}));

const WEBHOOK_URL = 'https://bytechef.example.com/webhooks/123/sse';

const capturedResolution: {current: ApprovalResolutionContextI | null} = {current: null};

function ApprovalResolutionCapture() {
    const approvalResolution = useApprovalResolution();

    useEffect(() => {
        capturedResolution.current = approvalResolution;
    }, [approvalResolution]);

    return null;
}

function renderProvider() {
    render(
        <AutomationChatProvider config={{webhookUrl: WEBHOOK_URL}}>
            <ApprovalResolutionCapture />
        </AutomationChatProvider>
    );
}

function dispatchApprovalRequest(event: Record<string, unknown>) {
    const options = useSSEMock.mock.lastCall![1] as UseSSEOptionsType;

    act(() => {
        options.eventHandlers!.approval_request(event);
    });
}

describe('AutomationChatProvider approvals', () => {
    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = globalThis.fetch;

    beforeEach(() => {
        globalThis.fetch = fetchSpy;

        useSSEMock.mockReturnValue({close: vi.fn(), connectionState: 'CLOSED', data: null, error: null});

        useChatStore.setState({messages: [], pendingApproval: null, resumeUrl: null});
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;
        capturedResolution.current = null;

        vi.clearAllMocks();
    });

    it('builds the resume URL from the resume id and the webhook origin, not from the form URL', () => {
        renderProvider();

        dispatchApprovalRequest({
            formTitle: 'Approve refund',
            formUrl: 'https://forms.example.com/resume/abc',
            resumeId: 'abc',
        });

        expect(useChatStore.getState().pendingApproval).toEqual({
            expiresAt: undefined,
            formDescription: undefined,
            formTitle: 'Approve refund',
            resumeUrl: 'https://bytechef.example.com/job/resume/abc',
        });
    });

    it('uses the resume URL carried by the event when present', () => {
        renderProvider();

        dispatchApprovalRequest({resumeId: 'abc', resumeUrl: 'https://api.example.com/job/resume/abc'});

        expect(useChatStore.getState().pendingApproval?.resumeUrl).toBe('https://api.example.com/job/resume/abc');
    });

    it('shows the approval as a message with the form link when it has inputs', () => {
        renderProvider();

        dispatchApprovalRequest({
            formTitle: 'Approve refund',
            formUrl: 'https://bytechef.example.com/resume/abc',
            inputs: [{name: 'amount'}],
            resumeId: 'abc',
        });

        expect(useChatStore.getState().pendingApproval).toBeNull();
        expect(useChatStore.getState().messages).toEqual([
            {
                content: '**Approve refund**\n\n[Open the approval form](https://bytechef.example.com/resume/abc)',
                role: 'assistant',
            },
        ]);
    });

    it('does not report the decision as recorded when the resume URL answers with an HTML page', async () => {
        renderProvider();

        dispatchApprovalRequest({formTitle: 'Approve refund', resumeId: 'abc'});

        const pendingApproval = useChatStore.getState().pendingApproval!;

        fetchSpy.mockResolvedValue(
            new Response('<html></html>', {headers: {'Content-Type': 'text/html'}, status: 200})
        );

        await expect(capturedResolution.current!.resolveApproval(pendingApproval, {approved: true})).rejects.toThrow(
            'not accepted'
        );

        expect(useChatStore.getState().pendingApproval).toEqual(pendingApproval);
        expect(useChatStore.getState().messages).toEqual([]);
    });

    it('records the decision when the resume URL answers 204', async () => {
        renderProvider();

        dispatchApprovalRequest({formTitle: 'Approve refund', resumeId: 'abc'});

        const pendingApproval = useChatStore.getState().pendingApproval!;

        fetchSpy.mockResolvedValue(new Response(null, {status: 204}));

        await act(async () => {
            await capturedResolution.current!.resolveApproval(pendingApproval, {approved: true});
        });

        expect(fetchSpy).toHaveBeenCalledWith(
            'https://bytechef.example.com/job/resume/abc',
            expect.objectContaining({body: JSON.stringify({approved: true}), method: 'POST'})
        );
        expect(useChatStore.getState().pendingApproval).toBeNull();
        expect(useChatStore.getState().messages).toEqual([
            {content: '**Approve refund** — approved.', role: 'assistant'},
        ]);
    });
});

describe('AutomationChatProvider answers', () => {
    const RESUME_URL = 'https://bytechef.example.com/job/resume/question';

    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = globalThis.fetch;

    function getOnNew() {
        const options = vi.mocked(useExternalStoreRuntime).mock.lastCall![0] as unknown as {
            onNew: (message: AppendMessage) => Promise<void>;
        };

        return options.onNew;
    }

    function createTextMessage(text: string) {
        return {content: [{text, type: 'text'}]} as unknown as AppendMessage;
    }

    beforeEach(() => {
        globalThis.fetch = fetchSpy;

        useSSEMock.mockReturnValue({close: vi.fn(), connectionState: 'CLOSED', data: null, error: null});

        useChatStore.setState({messages: [], pendingApproval: null, resumeUrl: RESUME_URL});
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;

        vi.clearAllMocks();
    });

    it('asks for an event stream and streams the resumed agent turn when the chat streams its responses', async () => {
        renderProvider();

        fetchSpy.mockResolvedValue(
            new Response('event: stream\ndata: Blue is a good choice.\n\n', {
                headers: {'Content-Type': 'text/event-stream'},
                status: 200,
            })
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        expect(fetchSpy).toHaveBeenCalledWith(
            RESUME_URL,
            expect.objectContaining({
                body: JSON.stringify({message: 'Blue'}),
                headers: {Accept: 'text/event-stream', 'Content-Type': 'application/json'},
                method: 'POST',
            })
        );

        const {messages, resumeUrl} = useChatStore.getState();

        expect(resumeUrl).toBeNull();
        expect(messages[messages.length - 1]).toEqual({content: 'Blue is a good choice.', role: 'assistant'});
    });

    it('does not report the answer as submitted when the resume URL answers with an HTML page', async () => {
        renderProvider();

        fetchSpy.mockResolvedValue(
            new Response('<html></html>', {headers: {'Content-Type': 'text/html'}, status: 200})
        );

        await act(async () => {
            await getOnNew()(createTextMessage('Blue'));
        });

        const {messages} = useChatStore.getState();

        expect(messages[messages.length - 1]).toEqual({
            content: 'Failed to submit your answer. Please try again.',
            role: 'assistant',
        });
    });
});
