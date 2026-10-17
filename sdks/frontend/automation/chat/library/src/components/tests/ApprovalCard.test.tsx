import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {ApprovalCard} from '@/components/ApprovalCard';
import {useChatStore} from '@/stores/useChatStore';

const PENDING_APPROVAL = {formTitle: 'Approve refund', resumeUrl: 'https://bytechef.example.com/job/resume/abc'};

describe('ApprovalCard without an approval resolution provider', () => {
    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = globalThis.fetch;

    beforeEach(() => {
        globalThis.fetch = fetchSpy;

        useChatStore.setState({messages: [], pendingApproval: PENDING_APPROVAL});
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;

        vi.clearAllMocks();
    });

    it('shows an error and keeps the card when the resume URL answers with an HTML page', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        fetchSpy.mockResolvedValue(
            new Response('<html></html>', {headers: {'Content-Type': 'text/html'}, status: 200})
        );

        render(<ApprovalCard />);

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        expect(await screen.findByRole('alert')).toHaveTextContent('Failed to submit your decision');
        expect(useChatStore.getState().pendingApproval).toEqual(PENDING_APPROVAL);
        expect(useChatStore.getState().messages).toEqual([]);

        consoleErrorSpy.mockRestore();
    });

    it('records the decision when the resume URL answers 204', async () => {
        fetchSpy.mockResolvedValue(new Response(null, {status: 204}));

        render(<ApprovalCard />);

        fireEvent.click(screen.getByRole('button', {name: 'Discard'}));

        await waitFor(() => expect(useChatStore.getState().pendingApproval).toBeNull());

        expect(useChatStore.getState().messages).toEqual([
            {content: '**Approve refund** — discarded.', role: 'assistant'},
        ]);
    });
});
