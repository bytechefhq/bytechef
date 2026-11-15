import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockUseDataTableLinkOptionsQuery: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useDataTableLinkOptionsQuery: hoisted.mockUseDataTableLinkOptionsQuery,
    };
});

import {LinkCell, LinkEditCell, getLinkDisplayText} from '../LinkCellRenderer';

beforeEach(() => {
    windowResizeObserver();

    hoisted.mockUseDataTableLinkOptionsQuery.mockReturnValue({
        data: {
            dataTableLinkOptions: [
                {id: '5', label: 'Acme'},
                {id: '6', label: 'Globex'},
            ],
        },
        isLoading: false,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('LinkCell', () => {
    it('renders the display value of the expanded row', () => {
        render(
            <LinkCell
                columnName="customer"
                displayColumnName="name"
                row={{customer: {externalId: null, id: '5', values: {name: 'Acme'}}, id: '1'}}
            />
        );

        expect(screen.getByText('Acme')).toBeInTheDocument();
    });

    it('renders #id without a display column', () => {
        render(
            <LinkCell
                columnName="customer"
                row={{customer: {externalId: null, id: '5', values: {name: 'Acme'}}, id: '1'}}
            />
        );

        expect(screen.getByText('#5')).toBeInTheDocument();
    });

    it('renders #id when the display value is empty', () => {
        expect(getLinkDisplayText({id: '5', values: {name: ''}}, 'name')).toBe('#5');
    });

    it('renders #id for an unexpanded id', () => {
        expect(getLinkDisplayText('5', 'name')).toBe('#5');
    });

    it('renders nothing for an empty link', () => {
        expect(getLinkDisplayText(null, 'name')).toBe('');
        expect(getLinkDisplayText(undefined, 'name')).toBe('');
    });
});

describe('LinkEditCell', () => {
    const renderEditCell = (onRowChange = vi.fn()) => {
        const row = {customer: {id: '5', values: {name: 'Acme'}}, id: '1'};

        render(
            <LinkEditCell
                columnName="customer"
                displayColumnName="name"
                environmentId="2"
                onRowChange={onRowChange}
                row={row}
                tableId="10"
            />
        );

        return {onRowChange, row};
    };

    it('sends null when cleared', async () => {
        const user = userEvent.setup();
        const {onRowChange, row} = renderEditCell();

        await user.click(screen.getByRole('button', {name: 'Clear'}));

        expect(onRowChange).toHaveBeenCalledWith({...row, customer: null}, true);
    });

    it('sends the target row id when an option is selected', async () => {
        const user = userEvent.setup();
        const {onRowChange, row} = renderEditCell();

        await user.click(screen.getByText('Globex'));

        expect(onRowChange).toHaveBeenCalledWith({...row, customer: '6'}, true);
    });

    it('queries the link options with a debounced search', async () => {
        const user = userEvent.setup();

        renderEditCell();

        await user.type(screen.getByPlaceholderText('Search customer'), 'glo');

        await waitFor(() => {
            expect(hoisted.mockUseDataTableLinkOptionsQuery).toHaveBeenLastCalledWith({
                columnName: 'customer',
                environmentId: '2',
                limit: 50,
                search: 'glo',
                tableId: '10',
            });
        });
    });
});
