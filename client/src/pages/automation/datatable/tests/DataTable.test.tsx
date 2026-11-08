import {act, render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {type ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTable from '../DataTable';
import {useDeleteDataTableAlertDialogStore} from '../stores/useDeleteDataTableAlertDialogStore';
import {useRenameDataTableDialogStore} from '../stores/useRenameDataTableDialogStore';

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, leftSidebarBody}: {children: ReactNode; leftSidebarBody: ReactNode}) => (
        <div>
            {leftSidebarBody}

            {children}
        </div>
    ),
}));

vi.mock('../components/DataTableHeader', () => ({
    default: () => null,
}));

vi.mock('../hooks/useDataTable', () => ({
    useDataTable: () => ({
        gridColumns: [],
        gridContainerRef: {current: null},
        gridRowsWithAdd: [],
        handleActivePositionChange: vi.fn(),
        handleCellClick: vi.fn(),
        handleCellKeyDown: vi.fn(),
        handleGridScroll: vi.fn(),
        handleRowsChange: vi.fn(),
        handleSelectedRowsChange: vi.fn(),
        rowsError: null,
        rowsLoading: false,
        tablesError: null,
        tablesLoading: false,
    }),
}));

vi.mock('../hooks/useDataTableLeftSidebar', () => ({
    default: () => ({
        error: null,
        filteredTables: [{baseName: 'Alpha', id: 'table-1'}],
        handleSearchChange: vi.fn(),
        isLoading: false,
        search: '',
    }),
}));

const renderDataTable = () =>
    render(
        <MemoryRouter initialEntries={['/automation/datatables/table-1']}>
            <Routes>
                <Route element={<DataTable />} path="/automation/datatables/:id" />
            </Routes>
        </MemoryRouter>
    );

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    useDeleteDataTableAlertDialogStore.getState().clearTableToDelete();
    useRenameDataTableDialogStore.getState().clearTableToRename();

    resetAll();
});

describe('DataTable', () => {
    it('should render a single delete table dialog', () => {
        renderDataTable();

        act(() => {
            useDeleteDataTableAlertDialogStore.getState().setTableToDelete('table-1', 'Alpha');
        });

        expect(screen.getAllByRole('alertdialog')).toHaveLength(1);
    });

    it('should render a single rename table dialog', () => {
        renderDataTable();

        act(() => {
            useRenameDataTableDialogStore.getState().setTableToRename('table-1', 'Alpha');
        });

        expect(screen.getAllByText('Rename Table')).toHaveLength(1);
    });
});
