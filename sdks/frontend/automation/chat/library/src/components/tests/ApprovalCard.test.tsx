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

describe('ApprovalCard with form inputs', () => {
    const fetchSpy = vi.fn<typeof fetch>();
    const originalFetch = globalThis.fetch;

    const FORM_APPROVAL = {
        formTitle: 'Approve refund',
        inputs: [
            {fieldLabel: 'Amount', fieldName: 'amount', fieldType: 9, required: true},
            {
                fieldLabel: 'Reason',
                fieldName: 'reason',
                fieldOptions: [
                    {label: 'Damaged', value: 'damaged'},
                    {label: 'Late', value: 'late'},
                ],
                fieldType: 7,
            },
            {defaultValue: 'r-1', fieldName: 'requestId', fieldType: 13},
        ],
        resumeUrl: 'https://bytechef.example.com/job/resume/abc',
    };

    beforeEach(() => {
        globalThis.fetch = fetchSpy;

        fetchSpy.mockResolvedValue(new Response(null, {status: 204}));

        useChatStore.setState({messages: [], pendingApproval: FORM_APPROVAL});
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;

        vi.clearAllMocks();
    });

    it('does not submit an approval while a required field is empty', () => {
        render(<ApprovalCard />);

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        expect(screen.getByText('This field is required')).toBeInTheDocument();
        expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('posts the field values, hidden fields and comment with the decision', async () => {
        render(<ApprovalCard />);

        fireEvent.change(screen.getByLabelText(/Amount/), {target: {value: '25'}});
        fireEvent.change(screen.getByLabelText(/Reason/), {target: {value: 'late'}});
        fireEvent.change(screen.getByLabelText('Comment (optional)'), {target: {value: 'ok'}});
        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        await waitFor(() => expect(useChatStore.getState().pendingApproval).toBeNull());

        expect(fetchSpy).toHaveBeenCalledWith(FORM_APPROVAL.resumeUrl, expect.objectContaining({method: 'POST'}));
        expect(JSON.parse(fetchSpy.mock.calls[0][1]!.body as string)).toEqual({
            amount: 25,
            approved: true,
            comment: 'ok',
            reason: 'late',
            requestId: 'r-1',
        });
    });

    it('discards without requiring the form fields', async () => {
        render(<ApprovalCard />);

        fireEvent.click(screen.getByRole('button', {name: 'Discard'}));

        await waitFor(() => expect(useChatStore.getState().pendingApproval).toBeNull());

        expect(JSON.parse(fetchSpy.mock.calls[0][1]!.body as string)).toEqual({
            approved: false,
            reason: '',
            requestId: 'r-1',
        });
    });

    it('does not submit an approval with an invalid email address', () => {
        useChatStore.setState({
            pendingApproval: {
                ...FORM_APPROVAL,
                inputs: [{fieldLabel: 'Approver', fieldName: 'approver', fieldType: 8}],
            },
        });

        render(<ApprovalCard />);

        fireEvent.change(screen.getByLabelText(/Approver/), {target: {value: 'not an email'}});
        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        expect(screen.getByText('Enter an email address')).toBeInTheDocument();
        expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('shows the field error the server returns', async () => {
        fetchSpy.mockResolvedValue(
            new Response(JSON.stringify({detail: "'Amount' must be at most 100"}), {
                headers: {'Content-Type': 'application/problem+json'},
                status: 400,
            })
        );

        render(<ApprovalCard />);

        fireEvent.change(screen.getByLabelText(/Amount/), {target: {value: '250'}});
        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        expect(await screen.findByRole('alert')).toHaveTextContent("'Amount' must be at most 100");
        expect(useChatStore.getState().pendingApproval).not.toBeNull();
    });

    it('posts an untouched date default exactly as defined', async () => {
        useChatStore.setState({
            pendingApproval: {
                ...FORM_APPROVAL,
                inputs: [
                    {
                        defaultValue: '2026-11-02T09:30:45Z',
                        fieldLabel: 'Start',
                        fieldName: 'startAt',
                        fieldType: 3,
                    },
                ],
            },
        });

        render(<ApprovalCard />);

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        await waitFor(() => expect(useChatStore.getState().pendingApproval).toBeNull());

        expect(JSON.parse(fetchSpy.mock.calls[0][1]!.body as string)).toEqual({
            approved: true,
            startAt: '2026-11-02T09:30:45Z',
        });
    });

    it('hides the comment box when the form defines its own comment field', () => {
        useChatStore.setState({
            pendingApproval: {
                ...FORM_APPROVAL,
                inputs: [{fieldLabel: 'Comment', fieldName: 'comment', fieldType: 5}],
            },
        });

        render(<ApprovalCard />);

        expect(screen.queryByLabelText('Comment (optional)')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Comment')).toBeInTheDocument();
    });
});
