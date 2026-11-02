import {describe, expect, it} from 'vitest';

import {
    ApprovalResumeError,
    formatApprovalRequest,
    formatAskUserQuestion,
    getApprovalResumeUrl,
    isAcceptedResumeResponse,
    isEventStreamResponse,
    toApprovalResumeError,
} from '@/utils/approval-utils';

describe('getApprovalResumeUrl', () => {
    it('uses the explicit resume URL of the event when present', () => {
        expect(
            getApprovalResumeUrl(
                {resumeId: 'abc', resumeUrl: 'https://resume.example.com/job/resume/abc'},
                'https://bytechef.example.com/webhooks/123/sse'
            )
        ).toBe('https://resume.example.com/job/resume/abc');
    });

    it('builds the resume URL from the resume id and the webhook origin', () => {
        expect(getApprovalResumeUrl({resumeId: 'abc'}, 'https://bytechef.example.com/webhooks/123/sse')).toBe(
            'https://bytechef.example.com/job/resume/abc'
        );
    });

    it('resolves a relative webhook URL against the base URL', () => {
        expect(getApprovalResumeUrl({resumeId: 'abc'}, '/webhooks/123/sse', 'https://host.example.com/page')).toBe(
            'https://host.example.com/job/resume/abc'
        );
    });

    it('encodes the resume id', () => {
        expect(getApprovalResumeUrl({resumeId: 'a/b c'}, 'https://bytechef.example.com/webhooks/123')).toBe(
            'https://bytechef.example.com/job/resume/a%2Fb%20c'
        );
    });

    it('returns null when there is neither a resume URL nor a resume id', () => {
        expect(getApprovalResumeUrl({resumeId: ''}, 'https://bytechef.example.com/webhooks/123')).toBeNull();
    });

    it('returns null when the webhook URL cannot be parsed', () => {
        expect(getApprovalResumeUrl({resumeId: 'abc'}, 'not a url', '')).toBeNull();
    });
});

describe('isAcceptedResumeResponse', () => {
    it('accepts 204', () => {
        expect(isAcceptedResumeResponse(new Response(null, {status: 204}))).toBe(true);
    });

    it('accepts 200 with an event stream', () => {
        expect(
            isAcceptedResumeResponse(new Response('', {headers: {'Content-Type': 'text/event-stream'}, status: 200}))
        ).toBe(true);
    });

    it('accepts 200 with JSON', () => {
        expect(
            isAcceptedResumeResponse(
                new Response('{}', {headers: {'Content-Type': 'application/json;charset=UTF-8'}, status: 200})
            )
        ).toBe(true);
    });

    it('rejects 200 with an HTML page', () => {
        expect(
            isAcceptedResumeResponse(
                new Response('<html></html>', {headers: {'Content-Type': 'text/html'}, status: 200})
            )
        ).toBe(false);
    });

    it('rejects error statuses', () => {
        expect(isAcceptedResumeResponse(new Response(null, {status: 410}))).toBe(false);
        expect(isAcceptedResumeResponse(new Response(null, {status: 409}))).toBe(false);
        expect(isAcceptedResumeResponse(new Response(null, {status: 500}))).toBe(false);
    });

    it('rejects other success statuses', () => {
        expect(isAcceptedResumeResponse(new Response(null, {status: 202}))).toBe(false);
    });
});

describe('isEventStreamResponse', () => {
    it('detects an event stream', () => {
        expect(isEventStreamResponse(new Response('', {headers: {'Content-Type': 'text/event-stream'}}))).toBe(true);
        expect(isEventStreamResponse(new Response('{}', {headers: {'Content-Type': 'application/json'}}))).toBe(false);
    });
});

describe('formatApprovalRequest', () => {
    it('formats the title, description, expiry and form link', () => {
        const expiresAt = '2026-10-16T10:00:00Z';

        expect(
            formatApprovalRequest({
                expiresAt,
                formDescription: 'Refund 20 EUR',
                formTitle: 'Approve refund',
                formUrl: 'https://bytechef.example.com/resume/abc',
                resumeId: 'abc',
            })
        ).toBe(
            `**Approve refund**\n\nRefund 20 EUR\n\nExpires ${new Date(expiresAt).toLocaleString()}` +
                '\n\n[Open the approval form](https://bytechef.example.com/resume/abc)'
        );
    });

    it('falls back to a default title', () => {
        expect(formatApprovalRequest({resumeId: 'abc'})).toBe('**Approval requested**');
    });
});

describe('formatAskUserQuestion', () => {
    it('returns an empty string without questions', () => {
        expect(formatAskUserQuestion({questions: []})).toBe('');
    });

    it('formats the header, question and numbered options', () => {
        expect(
            formatAskUserQuestion({
                questions: [
                    {
                        header: 'Color',
                        multiSelect: false,
                        options: [
                            {description: 'A calm color', label: 'Blue'},
                            {description: 'A warm color', label: 'Red'},
                        ],
                        question: 'Which color?',
                    },
                    {header: '', multiSelect: false, options: [], question: 'Anything else?'},
                ],
            })
        ).toBe('**Color**: Which color?\n  1. **Blue** — A calm color\n  2. **Red** — A warm color\n\nAnything else?');
    });
});

describe('toApprovalResumeError', () => {
    it('uses the problem detail of a 400 response', async () => {
        const response = new Response(JSON.stringify({detail: "'Approver' must be an email address"}), {
            headers: {'Content-Type': 'application/problem+json'},
            status: 400,
        });

        const error = await toApprovalResumeError(response);

        expect(error).toBeInstanceOf(ApprovalResumeError);
        expect(error.userMessage).toBe("'Approver' must be an email address");
        expect(error.message).toContain('status 400');
    });

    it('has no user message for a 400 response without a problem detail', async () => {
        const error = await toApprovalResumeError(new Response('<html></html>', {status: 400}));

        expect(error.userMessage).toBeNull();
    });

    it('explains expired, not yet suspended and failed runs', async () => {
        expect((await toApprovalResumeError(new Response(null, {status: 410}))).userMessage).toContain('expired');
        expect((await toApprovalResumeError(new Response(null, {status: 409}))).userMessage).toContain(
            'Still preparing'
        );
        expect((await toApprovalResumeError(new Response(null, {status: 422}))).userMessage).toContain(
            'workflow failed'
        );
    });

    it('has no user message for other statuses', async () => {
        expect((await toApprovalResumeError(new Response(null, {status: 500}))).userMessage).toBeNull();
    });
});
