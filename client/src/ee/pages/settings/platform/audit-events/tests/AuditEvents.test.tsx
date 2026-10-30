import {render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
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
    default: ({right, title}: {right?: ReactNode; title: string}) => (
        <header>
            <h1>{title}</h1>

            {right}
        </header>
    ),
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

const ONE_DAY_MILLISECONDS = 24 * 60 * 60 * 1000;

const lastQueryVariables = () => hoisted.useAuditEventsQueryMock.mock.calls.at(-1)?.[0];

const openFilterMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Filter options'}));

    return screen.findByRole('menu');
};

const selectMenuItem = async (name: string | RegExp) => {
    const menu = await openFilterMenu();

    await userEvent.click(within(menu).getByRole('menuitem', {name}));
};

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

    it('names the details button of each row after its event', () => {
        render(<AuditEvents />);

        expect(screen.getByRole('button', {name: 'View details of the PERMISSION_CHECK event'})).toBeInTheDocument();
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

    it('opens the event details with every recorded data entry when a row is clicked', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByText('alice@example.com'));

        const dialog = await screen.findByRole('dialog');

        expect(within(dialog).getByText('PERMISSION_CHECK')).toBeInTheDocument();
        expect(within(dialog).getByText('alice@example.com')).toBeInTheDocument();
        expect(within(dialog).getByText('DENIED')).toBeInTheDocument();
        expect(within(dialog).getByText('method')).toBeInTheDocument();
        expect(within(dialog).getByText('com.bytechef.ProjectFacade.delete')).toBeInTheDocument();
    });

    it('tells the viewer when no audit events match the filters', () => {
        hoisted.auditEventsQueryResult = {
            data: {auditEvents: {content: [], number: 0, size: 25, totalElements: 0, totalPages: 0}},
            isError: false,
            isLoading: false,
            refetch: hoisted.refetchMock,
        };

        render(<AuditEvents />);

        expect(screen.getByText('No audit events match the current filters.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
    });

    it('opens the Filter Audit Events menu from the header button', async () => {
        render(<AuditEvents />);

        const menu = await openFilterMenu();

        expect(within(menu).getByText('Filter Audit Events')).toBeInTheDocument();

        for (const menuItemName of [
            /^All$/,
            /^JOB_COMPLETED/,
            /^PERMISSION_CHECK/,
            /^All time/,
            /^Last 24 hours/,
            /^Last 7 days/,
            /^Last 30 days/,
            /Custom range/,
        ]) {
            expect(within(menu).getByRole('menuitem', {name: menuItemName})).toBeInTheDocument();
        }

        expect(within(menu).getByRole('menuitem', {name: 'Clear all filters'})).toHaveAttribute('data-disabled');
    });

    it('filters by the selected event type, returns to the first page and shows a chip', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await selectMenuItem(/^JOB_COMPLETED/);

        expect(lastQueryVariables()).toMatchObject({eventType: 'JOB_COMPLETED', page: 0});
        expect(screen.getByText('Event type: JOB_COMPLETED')).toBeInTheDocument();

        const menu = await openFilterMenu();

        expect(
            within(menu)
                .getByRole('menuitem', {name: /^JOB_COMPLETED/})
                .querySelector('svg')
        ).not.toBeNull();
        expect(within(menu).getByRole('menuitem', {name: /^All$/}).querySelector('svg')).toBeNull();
    });

    it('filters a date preset relative to now with no upper bound', async () => {
        render(<AuditEvents />);

        const beforeSelection = Date.now();

        await selectMenuItem(/^Last 7 days/);

        const afterSelection = Date.now();

        const {fromDate, toDate} = lastQueryVariables();

        expect(fromDate).toBeGreaterThanOrEqual(beforeSelection - 7 * ONE_DAY_MILLISECONDS);
        expect(fromDate).toBeLessThanOrEqual(afterSelection - 7 * ONE_DAY_MILLISECONDS);
        expect(toDate).toBeUndefined();
        expect(screen.getByText('Last 7 days')).toBeInTheDocument();
    });

    it('widens a custom range to the local start and end of the day and returns to the first page', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await selectMenuItem(/Custom range/);

        const [fromDateButton, toDateButton] = await screen.findAllByRole('button', {name: 'pick date'});

        await userEvent.click(fromDateButton);
        await userEvent.click(toDateButton);

        expect(lastQueryVariables()).toMatchObject({
            fromDate: new Date(2026, 0, 15, 0, 0, 0, 0).getTime(),
            page: 0,
            toDate: new Date(2026, 0, 15, 23, 59, 59, 999).getTime(),
        });

        const formattedDate = new Date(2026, 0, 15).toLocaleDateString();

        expect(screen.getByText(`From: ${formattedDate}`)).toBeInTheDocument();
        expect(screen.getByText(`To: ${formattedDate}`)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Clear from date filter'}));

        expect(lastQueryVariables()).toMatchObject({
            fromDate: undefined,
            toDate: new Date(2026, 0, 15, 23, 59, 59, 999).getTime(),
        });
    });

    it('replaces a custom range when a date preset is selected', async () => {
        render(<AuditEvents />);

        await selectMenuItem(/Custom range/);

        const [, toDateButton] = await screen.findAllByRole('button', {name: 'pick date'});

        await userEvent.click(toDateButton);
        await userEvent.keyboard('{Escape}');

        await selectMenuItem(/^Last 24 hours/);

        expect(lastQueryVariables()).toMatchObject({toDate: undefined});
        expect(screen.queryByText(/^To: /)).not.toBeInTheDocument();
        expect(screen.getByText('Last 24 hours')).toBeInTheDocument();
    });

    it('removes only the filter whose chip is cleared', async () => {
        render(<AuditEvents />);

        await selectMenuItem(/^JOB_COMPLETED/);
        await selectMenuItem(/^Last 30 days/);

        await userEvent.click(screen.getByRole('button', {name: 'Clear event type filter'}));

        expect(lastQueryVariables()).toMatchObject({eventType: undefined});
        expect(lastQueryVariables().fromDate).toBeDefined();
        expect(screen.queryByText('Event type: JOB_COMPLETED')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Clear date filter'}));

        expect(lastQueryVariables()).toMatchObject({fromDate: undefined, toDate: undefined});
        expect(screen.queryByRole('button', {name: 'Clear all'})).not.toBeInTheDocument();
    });

    it('trims typed filters and drops blank ones after the debounce', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByPlaceholderText('Principal (exact login)'), '  alice  ');
        await userEvent.type(screen.getByPlaceholderText('Search data'), '   ');

        expect(lastQueryVariables()).toMatchObject({principal: undefined});

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({dataSearch: undefined, principal: 'alice'}));
        expect(screen.getByText('Principal: alice')).toBeInTheDocument();
        expect(screen.queryByText(/^Data: /)).not.toBeInTheDocument();
    });

    it('empties the principal input when its chip is cleared', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByPlaceholderText('Principal (exact login)'), 'alice');
        await userEvent.type(screen.getByPlaceholderText('Search data'), 'createProject');

        await waitFor(() =>
            expect(lastQueryVariables()).toMatchObject({dataSearch: 'createProject', principal: 'alice'})
        );

        await userEvent.click(screen.getByRole('button', {name: 'Clear principal filter'}));

        expect(lastQueryVariables()).toMatchObject({dataSearch: 'createProject', principal: undefined});
        expect(screen.getByPlaceholderText('Principal (exact login)')).toHaveValue('');
        expect(screen.getByPlaceholderText('Search data')).toHaveValue('createProject');
    });

    it('tells the viewer in the filter menu when the event types could not be loaded', async () => {
        hoisted.auditEventTypesQueryResult = {data: undefined, isError: true};

        render(<AuditEvents />);

        const menu = await openFilterMenu();

        expect(within(menu).getByText('Event types could not be loaded.')).toBeInTheDocument();
    });

    it('highlights the filter button only while a filter is active', async () => {
        render(<AuditEvents />);

        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');

        await selectMenuItem(/^PERMISSION_CHECK/);

        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-surface-brand-primary');

        await userEvent.click(screen.getByRole('button', {name: 'Clear event type filter'}));

        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');
    });

    it('clears every filter with Clear all and requests the unfiltered first page', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByPlaceholderText('Principal (exact login)'), 'alice');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({principal: 'alice'}));

        await selectMenuItem(/^PERMISSION_CHECK/);
        await selectMenuItem(/^Last 24 hours/);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await userEvent.click(screen.getByRole('button', {name: 'Clear all'}));

        expect(lastQueryVariables()).toMatchObject({
            dataSearch: undefined,
            eventType: undefined,
            fromDate: undefined,
            page: 0,
            principal: undefined,
            toDate: undefined,
        });
        expect(screen.getByPlaceholderText('Principal (exact login)')).toHaveValue('');
        expect(screen.queryByText('Principal: alice')).not.toBeInTheDocument();
    });

    it('clears every filter from the Clear all filters menu item', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByPlaceholderText('Search data'), 'createProject');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({dataSearch: 'createProject'}));

        await selectMenuItem(/^JOB_COMPLETED/);
        await selectMenuItem('Clear all filters');

        expect(lastQueryVariables()).toMatchObject({dataSearch: undefined, eventType: undefined, page: 0});
        expect(screen.getByPlaceholderText('Search data')).toHaveValue('');
        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');
    });
});
