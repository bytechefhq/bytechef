import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AuditEvents from '../AuditEvents';

const hoisted = vi.hoisted(() => {
    return {
        auditEventTypesQueryResult: {} as Record<string, unknown>,
        auditEventsQueryResult: {} as Record<string, unknown>,
        refetchMock: vi.fn(),
        useAuditEventsQueryMock: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    AuditEventOutcome: {
        Allowed: 'ALLOWED',
        Denied: 'DENIED',
        Error: 'ERROR',
        RolledBack: 'ROLLED_BACK',
        Success: 'SUCCESS',
    },
    useAuditEventTypesQuery: () => hoisted.auditEventTypesQueryResult,
    useAuditEventsQuery: (variables: Record<string, unknown>) => {
        hoisted.useAuditEventsQueryMock(variables);

        return hoisted.auditEventsQueryResult;
    },
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {title: string}) => <h1>{title}</h1>,
}));

vi.mock('@/components/DatePicker/DatePicker', () => ({
    default: ({onChange}: {onChange: (date?: Date) => void}) => (
        <button onClick={() => onChange(new Date(2026, 0, 15, 13, 45))} type="button">
            pick date
        </button>
    ),
}));

const auditEvent = {
    data: [{key: 'method', value: 'com.bytechef.ProjectFacade.delete'}],
    eventDate: new Date(2026, 0, 15, 10, 0).getTime(),
    eventType: 'PERMISSION_CHECK',
    id: '1',
    outcome: 'DENIED',
    principal: 'alice@example.com',
};

const lastQueryVariables = () => hoisted.useAuditEventsQueryMock.mock.calls.at(-1)?.[0];

describe('AuditEvents', () => {
    beforeEach(() => {
        hoisted.useAuditEventsQueryMock.mockClear();
        hoisted.refetchMock.mockClear();
        hoisted.auditEventTypesQueryResult = {
            data: {auditEventTypes: ['JOB_COMPLETED', 'PERMISSION_CHECK']},
            isError: false,
        };
        hoisted.auditEventsQueryResult = {
            data: {auditEvents: {content: [auditEvent], number: 0, size: 25, totalElements: 60, totalPages: 3}},
            isError: false,
            isLoading: false,
            refetch: hoisted.refetchMock,
        };
    });

    it('renders each audit event with its outcome', () => {
        render(<AuditEvents />);

        expect(screen.getByText('alice@example.com')).toBeInTheDocument();
        expect(screen.getByText('PERMISSION_CHECK')).toBeInTheDocument();
        expect(screen.getByText('DENIED')).toBeInTheDocument();
    });

    it('disables Previous on the first page and requests the next page on Next', async () => {
        render(<AuditEvents />);

        expect(screen.getByRole('button', {name: 'Previous'})).toBeDisabled();

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        expect(lastQueryVariables()).toMatchObject({page: 1, size: 25});
        expect(screen.getByText('Page 2 of 3')).toBeInTheDocument();
    });

    it('keeps the requested page once the filter debounce settles', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await new Promise((resolve) => setTimeout(resolve, 400));

        expect(lastQueryVariables()).toMatchObject({page: 1});
    });

    it('disables Next on the last page', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
    });

    it('widens picked dates to the local start and end of the day and returns to the first page', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        const [fromDateButton, toDateButton] = screen.getAllByRole('button', {name: 'pick date'});

        await userEvent.click(fromDateButton);
        await userEvent.click(toDateButton);

        expect(lastQueryVariables()).toMatchObject({
            fromDate: new Date(2026, 0, 15, 0, 0, 0, 0).getTime(),
            page: 0,
            toDate: new Date(2026, 0, 15, 23, 59, 59, 999).getTime(),
        });
    });

    it('trims typed filters and drops blank ones after the debounce', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByPlaceholderText('e.g. admin@localhost.com'), '  alice  ');
        await userEvent.type(screen.getByPlaceholderText('match any data value'), '   ');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({dataSearch: undefined, principal: 'alice'}));
    });

    it('tells the viewer when the event types could not be loaded', () => {
        hoisted.auditEventTypesQueryResult = {data: undefined, isError: true};

        render(<AuditEvents />);

        expect(screen.getByText('Event types could not be loaded.')).toBeInTheDocument();
    });

    it('shows an error with a retry instead of an empty table when loading fails', async () => {
        hoisted.auditEventsQueryResult = {
            data: {auditEvents: {content: [auditEvent], number: 0, size: 25, totalElements: 60, totalPages: 3}},
            isError: true,
            isLoading: false,
            refetch: hoisted.refetchMock,
        };

        render(<AuditEvents />);

        expect(screen.getByText('Audit events could not be loaded.')).toBeInTheDocument();
        expect(screen.queryByText('No audit events match the current filters.')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(hoisted.refetchMock).toHaveBeenCalled();
    });
});
