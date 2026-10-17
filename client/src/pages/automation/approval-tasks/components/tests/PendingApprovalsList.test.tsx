import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {authenticationState, usePendingApprovalsQueryMock} = vi.hoisted(() => ({
    authenticationState: {account: {authorities: ['ROLE_ADMIN']} as {authorities?: string[]} | null},
    usePendingApprovalsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    usePendingApprovalsQuery: usePendingApprovalsQueryMock,
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: (selector: (state: typeof authenticationState) => unknown) => selector(authenticationState),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

import PendingApprovalsList from '../PendingApprovalsList';

describe('PendingApprovalsList', () => {
    beforeEach(() => {
        authenticationState.account = {authorities: ['ROLE_ADMIN']};
        usePendingApprovalsQueryMock.mockReset();
    });

    it('lists the pending approvals of the current environment for an admin', () => {
        usePendingApprovalsQueryMock.mockReturnValue({
            data: {
                pendingApprovals: [
                    {
                        createdDate: '2026-10-01T10:00:00Z',
                        expiresAt: '2026-10-02T10:00:00Z',
                        formUrl: 'https://example.com/approvals/abc',
                        jobId: '1',
                        workflowLabel: 'Approve invoice',
                    },
                    null,
                    {createdDate: 'not-a-date', expiresAt: null, formUrl: null, jobId: '2', workflowLabel: 'Refund'},
                ],
            },
        });

        render(<PendingApprovalsList />);

        expect(usePendingApprovalsQueryMock).toHaveBeenCalledWith(
            {environmentId: 2},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getByText('Pending run approvals (2)')).toBeInTheDocument();
        expect(screen.getByText('Approve invoice')).toBeInTheDocument();
        expect(screen.getByText('Refund')).toBeInTheDocument();
        expect(screen.getAllByText(/^started /)).toHaveLength(1);
        expect(screen.getAllByText(/^expires /)).toHaveLength(1);

        const formLink = screen.getByRole('link', {name: /Open form/});

        expect(formLink).toHaveAttribute('href', 'https://example.com/approvals/abc');
        expect(formLink).toHaveAttribute('target', '_blank');
    });

    it('renders nothing when there are no pending approvals', () => {
        usePendingApprovalsQueryMock.mockReturnValue({data: {pendingApprovals: []}});

        const {container} = render(<PendingApprovalsList />);

        expect(container).toBeEmptyDOMElement();
    });

    it('does not query or render for a non-admin', () => {
        authenticationState.account = {authorities: ['ROLE_USER']};

        usePendingApprovalsQueryMock.mockReturnValue({
            data: {pendingApprovals: [{jobId: '1', workflowLabel: 'Approve invoice'}]},
        });

        const {container} = render(<PendingApprovalsList />);

        expect(usePendingApprovalsQueryMock).toHaveBeenCalledWith(
            {environmentId: 2},
            expect.objectContaining({enabled: false})
        );
        expect(container).toBeEmptyDOMElement();
    });
});
