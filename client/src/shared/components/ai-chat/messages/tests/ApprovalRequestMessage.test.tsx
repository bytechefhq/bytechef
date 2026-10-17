import {ApprovalResolutionContext, ResumeError} from '@/shared/components/ai-chat/approvalResolutionContext';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mutateAsyncMock} = vi.hoisted(() => ({
    mutateAsyncMock: vi.fn(),
}));

vi.mock('@/shared/components/approval-form/ApprovalForm', () => ({
    default: ({
        id,
        submitHandler,
    }: {
        id: string | undefined;
        submitHandler?: (data: Record<string, unknown>, approved: boolean) => Promise<void>;
    }) => (
        <div data-testid="approval-form">
            {id}

            {submitHandler && (
                <button onClick={() => submitHandler({amount: 5}, false)} type="button">
                    Submit form
                </button>
            )}
        </div>
    ),
}));

vi.mock('@/shared/mutations/platform/resumeJobs.mutations', () => ({
    useResumeJobMutation: () => ({mutateAsync: mutateAsyncMock}),
}));

import ApprovalRequestMessage from '../ApprovalRequestMessage';

const withResolution = (resolveApproval: (id: string, payload: Record<string, unknown>) => Promise<void>) =>
    function Wrapper({children}: {children: ReactNode}) {
        return (
            <ApprovalResolutionContext.Provider value={{resolveApproval}}>
                {children}
            </ApprovalResolutionContext.Provider>
        );
    };

describe('ApprovalRequestMessage', () => {
    beforeEach(() => {
        mutateAsyncMock.mockReset();
    });

    it('renders a self-contained approve/discard card for field-less approvals', () => {
        render(
            <ApprovalRequestMessage
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                {...({data: {formTitle: 'Approve tool call', kind: 'approval-request', resumeId: 'abc123'}} as any)}
            />
        );

        expect(screen.getByText('Approval required')).toBeInTheDocument();
        expect(screen.getByText('Approve tool call')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Approve'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Discard'})).toBeEnabled();
        expect(screen.queryByTestId('approval-form')).not.toBeInTheDocument();
    });

    it('embeds the approval form when the approval carries form fields', () => {
        render(
            <ApprovalRequestMessage
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                {...({data: {hasInputs: true, kind: 'approval-request', resumeId: 'abc123'}} as any)}
            />
        );

        expect(screen.getByTestId('approval-form')).toHaveTextContent('abc123');
        expect(screen.queryByRole('button', {name: 'Approve'})).not.toBeInTheDocument();
    });

    it('renders nothing without a resume id', () => {
        const {container} = render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <ApprovalRequestMessage {...({data: {kind: 'approval-request', resumeId: ''}} as any)} />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('shows the continuing state only after a successful resolution', async () => {
        const resolveApproval = vi.fn().mockResolvedValue(undefined);

        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <ApprovalRequestMessage {...({data: {kind: 'approval-request', resumeId: 'abc123'}} as any)} />,
            {wrapper: withResolution(resolveApproval)}
        );

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        await waitFor(() => expect(screen.getByText('Approved — the workflow is continuing.')).toBeInTheDocument());
        expect(resolveApproval).toHaveBeenCalledWith('abc123', {approved: true});
    });

    it('does NOT claim success when the resolution fails, and surfaces the expiry error', async () => {
        const resolveApproval = vi.fn().mockRejectedValue(new ResumeError(410));

        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <ApprovalRequestMessage {...({data: {kind: 'approval-request', resumeId: 'abc123'}} as any)} />,
            {wrapper: withResolution(resolveApproval)}
        );

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('expired'));

        expect(screen.queryByText('Approved — the workflow is continuing.')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Approve'})).toBeEnabled();
    });

    it('resumes the job directly with the trimmed comment when no resolution context is present', async () => {
        mutateAsyncMock.mockResolvedValue(undefined);

        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <ApprovalRequestMessage {...({data: {kind: 'approval-request', resumeId: 'abc123'}} as any)} />
        );

        fireEvent.change(screen.getByLabelText('Comment (optional)'), {target: {value: '  not this quarter  '}});
        fireEvent.click(screen.getByRole('button', {name: 'Discard'}));

        await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Discarded.'));
        expect(mutateAsyncMock).toHaveBeenCalledWith({
            approved: false,
            data: {comment: 'not this quarter'},
            id: 'abc123',
        });
    });

    it('shows the expiry until the approval is resolved and ignores an unparseable one', async () => {
        mutateAsyncMock.mockResolvedValue(undefined);

        const {rerender} = render(
            <ApprovalRequestMessage
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                {...({data: {expiresAt: 'soon', kind: 'approval-request', resumeId: 'abc123'}} as any)}
            />
        );

        expect(screen.queryByText(/^Expires /)).not.toBeInTheDocument();

        rerender(
            <ApprovalRequestMessage
                {...({
                    data: {expiresAt: '2026-10-20T10:00:00Z', kind: 'approval-request', resumeId: 'abc123'},
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any)}
            />
        );

        expect(screen.getByText(/^Expires /)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        await waitFor(() => expect(screen.getByRole('status')).toBeInTheDocument());
        expect(screen.queryByText(/^Expires /)).not.toBeInTheDocument();
    });

    it('falls back to a generic error and links the approval form when submitting fails', async () => {
        mutateAsyncMock.mockRejectedValue('network down');

        render(
            <ApprovalRequestMessage
                {...({
                    data: {
                        formDescription: 'Refund over limit',
                        formUrl: 'https://example.com/approvals/abc123',
                        kind: 'approval-request',
                        resumeId: 'abc123',
                    },
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any)}
            />
        );

        expect(screen.getByText('Approval requested')).toBeInTheDocument();
        expect(screen.getByText('Refund over limit')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));

        await waitFor(() =>
            expect(screen.getByRole('alert')).toHaveTextContent('Failed to submit your decision. Please try again.')
        );
        expect(screen.getByRole('link', {name: 'Open the approval form'})).toHaveAttribute(
            'href',
            'https://example.com/approvals/abc123'
        );
    });

    it('routes an embedded form submission through the resolution context', async () => {
        const resolveApproval = vi.fn().mockResolvedValue(undefined);

        render(
            <ApprovalRequestMessage
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                {...({data: {hasInputs: true, kind: 'approval-request', resumeId: 'abc123'}} as any)}
            />,
            {wrapper: withResolution(resolveApproval)}
        );

        fireEvent.click(screen.getByRole('button', {name: 'Submit form'}));

        await waitFor(() => expect(resolveApproval).toHaveBeenCalledWith('abc123', {amount: 5, approved: false}));
        expect(mutateAsyncMock).not.toHaveBeenCalled();
    });
});
