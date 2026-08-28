import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTable from '../DataTable';

const hoisted = vi.hoisted(() => {
    return {
        mockRegisterPostTurn: vi.fn(),
        mockSetContext: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockUseDataTable: vi.fn(),
        mockUseParams: vi.fn(),
    };
});

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {
        ...actual,
        useParams: hoisted.mockUseParams,
    };
});

vi.mock('../hooks/useDataTable', () => ({
    useDataTable: hoisted.mockUseDataTable,
}));

vi.mock('../stores/useSelectedRowsStore', () => ({
    useSelectedRowsStore: () => ({
        selectedRows: new Set(),
        setSelectedRows: vi.fn(),
    }),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', async () => {
    const actual = await vi.importActual<typeof import('@/shared/components/copilot/stores/useCopilotStore')>(
        '@/shared/components/copilot/stores/useCopilotStore'
    );

    return {
        ...actual,
        useCopilotStore: (selector: (state: {setContext: typeof hoisted.mockSetContext}) => unknown) =>
            selector({setContext: hoisted.mockSetContext}),
    };
});

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {setCopilotPanelOpen: typeof hoisted.mockSetCopilotPanelOpen}) => unknown) =>
        selector({setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen}),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPostTurnRegistry', () => ({
    default: (selector: (state: {register: typeof hoisted.mockRegisterPostTurn}) => unknown) =>
        selector({register: hoisted.mockRegisterPostTurn}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/components/PageLoader', () => ({
    default: ({children, errors, loading}: {children: React.ReactNode; errors: unknown[]; loading: boolean}) =>
        loading ? (
            <div data-testid="page-loader-loading">Loading...</div>
        ) : errors.filter(Boolean).length > 0 ? (
            <div data-testid="page-loader-error">Error</div>
        ) : (
            <div data-testid="page-loader-content">{children}</div>
        ),
}));

vi.mock('../components/DataTableHeader', () => ({
    default: ({onAskCopilot}: {onAskCopilot?: () => void}) => (
        <header data-testid="data-table-header">
            {onAskCopilot && (
                <button onClick={onAskCopilot} type="button">
                    Ask Copilot
                </button>
            )}
        </header>
    ),
}));

vi.mock('../components/DataTableLeftSidebar', () => ({
    default: () => <nav data-testid="data-table-left-sidebar">Sidebar</nav>,
}));

vi.mock('@/shared/components/data-tables/components/CreateDataTableDialog', () => ({
    default: () => <div data-testid="create-data-table-dialog" />,
}));

vi.mock('../components/AddDataTableColumnDialog', () => ({
    default: () => <div data-testid="add-data-table-column-dialog" />,
}));

vi.mock('../components/RenameDataTableColumnDialog', () => ({
    default: () => <div data-testid="rename-data-table-column-dialog" />,
}));

vi.mock('../components/DeleteDataTableColumnDialog', () => ({
    default: () => <div data-testid="delete-data-table-column-dialog" />,
}));

vi.mock('../components/DeleteDataTableRowsDialog', () => ({
    default: () => <div data-testid="delete-data-table-rows-dialog" />,
}));

vi.mock('../components/RenameDataTableDialog', () => ({
    default: () => <div data-testid="rename-data-table-dialog" />,
}));

vi.mock('../components/DeleteDataTableAlertDialog', () => ({
    default: () => <div data-testid="delete-data-table-alert-dialog" />,
}));

vi.mock('../components/ImportDataTableCsvDialog', () => ({
    default: () => <div data-testid="import-data-table-csv-dialog" />,
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {position?: string; title?: string}) => <header data-testid="sidebar-header">{title}</header>,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({
        children,
        header,
        leftSidebarBody,
        leftSidebarHeader,
    }: {
        children: React.ReactNode;
        header: React.ReactNode;
        leftSidebarBody: React.ReactNode;
        leftSidebarHeader: React.ReactNode;
        leftSidebarWidth: string;
    }) => (
        <div data-testid="layout-container">
            <div data-testid="layout-header">{header}</div>

            <div data-testid="layout-sidebar-header">{leftSidebarHeader}</div>

            <div data-testid="layout-sidebar-body">{leftSidebarBody}</div>

            <div data-testid="layout-content">{children}</div>
        </div>
    ),
}));

vi.mock('react-data-grid', () => ({
    DataGrid: () => <div data-testid="data-grid" />,
}));

const defaultMockReturn = {
    gridColumns: [],
    gridRowsWithAdd: [],
    handleGridScroll: vi.fn(),
    handleRowsChange: vi.fn(),
    handleSelectedRowsChange: vi.fn(),
    rowsError: null,
    rowsLoading: false,
    tablesError: null,
    tablesLoading: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseParams.mockReturnValue({id: 'table-1'});
    hoisted.mockUseDataTable.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTable', () => {
    it('renders layout container', () => {
        render(<DataTable />);

        expect(screen.getByTestId('layout-container')).toBeInTheDocument();
    });

    it('opens copilot scoped to the current data table', async () => {
        render(<DataTable />);

        await userEvent.click(screen.getByRole('button', {name: /ask copilot/i}));

        expect(hoisted.mockSetContext).toHaveBeenCalledWith(
            expect.objectContaining({
                mode: MODE.ASK,
                parameters: expect.objectContaining({dataTableId: 'table-1'}),
                source: Source.DATA_TABLE,
            })
        );
        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });
});
