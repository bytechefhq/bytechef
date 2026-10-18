import {ApprovalResolutionContext} from '@/shared/components/ai-chat/approvalResolutionContext';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps, ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import ToolApprovalRequestMessage, {ToolApprovalRequestDataI} from '../ToolApprovalRequestMessage';

type ToolApprovalRequestPropsType = ComponentProps<typeof ToolApprovalRequestMessage>;

const toProps = (data: object) => ({data}) as unknown as ToolApprovalRequestPropsType;

const withResolution = (
    resolveToolApproval: (
        approvalId: number,
        approved: boolean,
        comment?: string
    ) => Promise<{executionError?: string | null; status: string}>
) =>
    function Wrapper({children}: {children: ReactNode}) {
        return (
            <ApprovalResolutionContext.Provider value={{resolveToolApproval}}>
                {children}
            </ApprovalResolutionContext.Provider>
        );
    };

describe('ToolApprovalRequestMessage', () => {
    it('renders the tool name and arguments', () => {
        render(<ToolApprovalRequestMessage {...toProps(data())} />, {wrapper: withResolution(vi.fn())});

        expect(screen.getByText(/gmail\/sendEmail/)).toBeInTheDocument();
        expect(screen.getByText('to')).toBeInTheDocument();
        expect(screen.getByText('a@b.c')).toBeInTheDocument();
    });

    it('approves through the context and shows the approved state', async () => {
        const resolveToolApproval = vi.fn().mockResolvedValue({status: 'APPROVED'});

        render(<ToolApprovalRequestMessage {...toProps(data())} />, {
            wrapper: withResolution(resolveToolApproval),
        });

        await userEvent.click(screen.getByRole('button', {name: 'Approve'}));

        expect(resolveToolApproval).toHaveBeenCalledWith(1234, true, undefined);
        expect(await screen.findByText(/Approved/)).toBeInTheDocument();
    });

    /**
     * Regression test for the card claiming success on a failed execution: approving a call whose tool then throws
     * still resolves the mutation successfully (2xx), so the old code — which set the local "resolved" flag from the
     * `approved` boolean it already knew, ignoring what the mutation actually returned — rendered "Approved — the
     * tool ran." here. This asserts on the row's real returned status instead.
     */
    it('shows the failed state when the resolved approval failed to execute', async () => {
        const resolveToolApproval = vi.fn().mockResolvedValue({executionError: 'boom', status: 'FAILED'});

        render(<ToolApprovalRequestMessage {...toProps(data())} />, {
            wrapper: withResolution(resolveToolApproval),
        });

        await userEvent.click(screen.getByRole('button', {name: 'Approve'}));

        expect(await screen.findByText(/Failed: boom/)).toBeInTheDocument();
        expect(screen.queryByText(/^Approved/)).not.toBeInTheDocument();
    });

    it('rejects with a comment', async () => {
        const resolveToolApproval = vi.fn().mockResolvedValue({status: 'REJECTED'});

        render(<ToolApprovalRequestMessage {...toProps(data())} />, {
            wrapper: withResolution(resolveToolApproval),
        });

        await userEvent.type(screen.getByLabelText(/Comment/), 'not now');
        await userEvent.click(screen.getByRole('button', {name: 'Reject'}));

        expect(resolveToolApproval).toHaveBeenCalledWith(1234, false, 'not now');
    });

    it('renders a resolved overlay when the status is known', () => {
        render(<ToolApprovalRequestMessage {...toProps({...data(), resolvedStatus: 'SUPERSEDED'})} />, {
            wrapper: withResolution(vi.fn()),
        });

        expect(screen.getByText(/Superseded/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Approve'})).not.toBeInTheDocument();
    });
});

const data = (): ToolApprovalRequestDataI => ({
    approvalId: 1234,
    arguments: {to: 'a@b.c'},
    awaitingApproval: true,
    componentName: 'gmail',
    kind: 'tool-approval-request',
    toolName: 'sendEmail',
});
