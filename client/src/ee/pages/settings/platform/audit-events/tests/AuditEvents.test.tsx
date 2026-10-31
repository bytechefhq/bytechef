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
    PlatformType: {
        Automation: 'AUTOMATION',
        Embedded: 'EMBEDDED',
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
    platform: 'AUTOMATION',
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

    it('shows the platform column between Principal and Event Type', () => {
        render(<AuditEvents />);

        const columnHeaders = screen.getAllByRole('columnheader').map((columnHeader) => columnHeader.textContent);

        expect(columnHeaders.slice(0, 4)).toEqual(['Date', 'Principal', 'Platform', 'Event Type']);
        expect(screen.getByRole('cell', {name: 'Automation'})).toBeInTheDocument();
    });

    it('renders Embedded and a dash for an event without a platform', () => {
        hoisted.auditEventsQueryResult = {
            data: {
                auditEvents: {
                    content: [
                        {...auditEvent, id: '1', platform: 'EMBEDDED'},
                        {...auditEvent, id: '2', outcome: 'ALLOWED', platform: null},
                    ],
                    number: 0,
                    size: 25,
                    totalElements: 2,
                    totalPages: 1,
                },
            },
            isError: false,
            isLoading: false,
            refetch: hoisted.refetchMock,
        };

        render(<AuditEvents />);

        const [, embeddedRow, platformlessRow] = screen.getAllByRole('row');

        expect(within(embeddedRow).getAllByRole('cell')[2]).toHaveTextContent('Embedded');
        expect(within(platformlessRow).getAllByRole('cell')[2]).toHaveTextContent('—');
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
        expect(within(dialog).getByText('Platform')).toBeInTheDocument();
        expect(within(dialog).getByText('Automation')).toBeInTheDocument();
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

        expect(within(menu).getAllByRole('menuitem', {name: /^All$/})).toHaveLength(2);

        for (const menuItemName of [
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

    it('scrolls the event type list on its own and shows each full name as a title', async () => {
        hoisted.auditEventTypesQueryResult = {
            data: {auditEventTypes: ['JOB_COMPLETED', 'WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED']},
            isError: false,
        };

        render(<AuditEvents />);

        const menu = await openFilterMenu();

        const eventTypeGroup = within(menu).getByRole('group', {name: 'Event types'});

        expect(eventTypeGroup).toHaveClass('max-h-64', 'overflow-y-auto');
        expect(within(eventTypeGroup).getByRole('menuitem', {name: /^All$/})).toBeInTheDocument();
        expect(within(eventTypeGroup).getByText('WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED')).toHaveAttribute(
            'title',
            'WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED'
        );
        expect(within(eventTypeGroup).queryByRole('menuitem', {name: /^Last 7 days/})).not.toBeInTheDocument();
        expect(within(menu).getByRole('menuitem', {name: 'Clear all filters'})).toBeInTheDocument();
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
        expect(
            within(within(menu).getByRole('group', {name: 'Event types'}))
                .getByRole('menuitem', {name: /^All$/})
                .querySelector('svg')
        ).toBeNull();
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

    it('places the search box in the header before the filter button', () => {
        render(<AuditEvents />);

        const header = screen.getByRole('banner');
        const searchInput = within(header).getByRole('textbox', {name: 'Search'});
        const filterButton = within(header).getByRole('button', {name: 'Filter options'});

        expect(searchInput).toHaveAttribute('placeholder', 'Search');
        expect(searchInput.compareDocumentPosition(filterButton) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('searches with the trimmed text after the debounce and returns to the first page', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await userEvent.type(screen.getByRole('textbox', {name: 'Search'}), '  alice  ');

        expect(lastQueryVariables()).toMatchObject({page: 1, search: undefined});

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({page: 0, search: 'alice'}));
        expect(screen.queryByText(/^Search: /)).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');
    });

    it('drops a blank search after the debounce', async () => {
        render(<AuditEvents />);

        const searchInput = screen.getByRole('textbox', {name: 'Search'});

        await userEvent.type(searchInput, 'alice');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({search: 'alice'}));

        await userEvent.clear(searchInput);
        await userEvent.type(searchInput, '   ');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({search: undefined}));
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

    it('clears the menu filters with Clear all and keeps the search', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByRole('textbox', {name: 'Search'}), 'alice');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({search: 'alice'}));

        await selectMenuItem(/^PERMISSION_CHECK/);
        await selectMenuItem(/^Last 24 hours/);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await userEvent.click(screen.getByRole('button', {name: 'Clear all'}));

        expect(lastQueryVariables()).toMatchObject({
            eventType: undefined,
            fromDate: undefined,
            page: 0,
            search: 'alice',
            toDate: undefined,
        });
        expect(screen.getByRole('textbox', {name: 'Search'})).toHaveValue('alice');
    });

    it('clears the menu filters from the Clear all filters menu item', async () => {
        render(<AuditEvents />);

        await userEvent.type(screen.getByRole('textbox', {name: 'Search'}), 'createProject');

        await waitFor(() => expect(lastQueryVariables()).toMatchObject({search: 'createProject'}));

        await selectMenuItem(/^JOB_COMPLETED/);
        await selectMenuItem('Clear all filters');

        expect(lastQueryVariables()).toMatchObject({eventType: undefined, page: 0, search: 'createProject'});
        expect(screen.getByRole('textbox', {name: 'Search'})).toHaveValue('createProject');
        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');
    });

    it('lists All, Automation and Embedded in the Platform section with All checked', async () => {
        render(<AuditEvents />);

        const menu = await openFilterMenu();

        const platformGroup = within(menu).getByRole('group', {name: 'Platforms'});

        expect(within(platformGroup).getByText('Platform')).toBeInTheDocument();
        expect(
            within(platformGroup)
                .getAllByRole('menuitem')
                .map((menuItem) => menuItem.textContent)
        ).toEqual(['All', 'Automation', 'Embedded']);
        expect(within(platformGroup).getByRole('menuitem', {name: /^All$/}).querySelector('svg')).not.toBeNull();
    });

    it('filters by the selected platform, returns to the first page and shows a chip', async () => {
        render(<AuditEvents />);

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        await selectMenuItem(/^Embedded/);

        expect(lastQueryVariables()).toMatchObject({page: 0, platform: 'EMBEDDED'});
        expect(screen.getByText('Platform: Embedded')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-surface-brand-primary');

        const menu = await openFilterMenu();

        const platformGroup = within(menu).getByRole('group', {name: 'Platforms'});

        expect(
            within(platformGroup)
                .getByRole('menuitem', {name: /^Embedded/})
                .querySelector('svg')
        ).not.toBeNull();
        expect(within(platformGroup).getByRole('menuitem', {name: /^All$/}).querySelector('svg')).toBeNull();
    });

    it('removes only the platform filter when its chip is cleared', async () => {
        render(<AuditEvents />);

        await selectMenuItem(/^Automation/);
        await selectMenuItem(/^JOB_COMPLETED/);

        await userEvent.click(screen.getByRole('button', {name: 'Clear platform filter'}));

        expect(lastQueryVariables()).toMatchObject({eventType: 'JOB_COMPLETED', platform: undefined});
        expect(screen.queryByText('Platform: Automation')).not.toBeInTheDocument();
        expect(screen.getByText('Event type: JOB_COMPLETED')).toBeInTheDocument();
    });

    it('turns the filter highlight off once the platform chip is cleared', async () => {
        render(<AuditEvents />);

        await selectMenuItem(/^Automation/);

        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-surface-brand-primary');

        await userEvent.click(screen.getByRole('button', {name: 'Clear platform filter'}));

        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');
        expect(screen.queryByRole('button', {name: 'Clear all'})).not.toBeInTheDocument();
    });

    it('clears the platform filter with Clear all', async () => {
        render(<AuditEvents />);

        await selectMenuItem(/^Embedded/);
        await selectMenuItem(/^Last 24 hours/);

        await userEvent.click(screen.getByRole('button', {name: 'Clear all'}));

        expect(lastQueryVariables()).toMatchObject({fromDate: undefined, page: 0, platform: undefined});
        expect(screen.queryByText('Platform: Embedded')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Filter options'})).toHaveClass('bg-transparent');
    });

    it('clears the platform filter from the Clear all filters menu item', async () => {
        render(<AuditEvents />);

        await selectMenuItem(/^Automation/);
        await selectMenuItem('Clear all filters');

        expect(lastQueryVariables()).toMatchObject({platform: undefined});
        expect(screen.queryByText('Platform: Automation')).not.toBeInTheDocument();
    });
});
