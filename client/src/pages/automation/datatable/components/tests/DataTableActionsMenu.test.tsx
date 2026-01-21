import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTableActionsMenu from '../DataTableActionsMenu';

const defaultProps = {
    onDeleteTable: vi.fn(),
    onExportCsv: vi.fn(),
    onImportCsv: vi.fn(),
    onRenameTable: vi.fn(),
    tableId: 'table-123',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DataTableActionsMenu', () => {
    describe('rendering', () => {
        it('should render the menu trigger button', () => {
            render(<DataTableActionsMenu {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'More actions'})).toBeInTheDocument();
        });

        it('should not show menu items initially', () => {
            render(<DataTableActionsMenu {...defaultProps} />);

            expect(screen.queryByText('Import CSV')).not.toBeInTheDocument();
            expect(screen.queryByText('Export CSV')).not.toBeInTheDocument();
        });
    });

    describe('menu items when tableId is provided', () => {
        it('should show all menu items when trigger is clicked', async () => {
            const user = userEvent.setup();

            render(<DataTableActionsMenu {...defaultProps} />);

            const triggerButton = screen.getByRole('button', {name: 'More actions'});

            await user.click(triggerButton);

            expect(screen.getByText('Import CSV')).toBeInTheDocument();
            expect(screen.getByText('Export CSV')).toBeInTheDocument();
            expect(screen.getByText('Rename Table')).toBeInTheDocument();
            expect(screen.getByText('Delete Table')).toBeInTheDocument();
        });
    });

    describe('menu items when tableId is not provided', () => {
        it('should hide Rename and Delete options when no tableId', async () => {
            const user = userEvent.setup();

            render(<DataTableActionsMenu {...defaultProps} tableId={undefined} />);

            const triggerButton = screen.getByRole('button', {name: 'More actions'});

            await user.click(triggerButton);

            expect(screen.getByText('Import CSV')).toBeInTheDocument();
            expect(screen.getByText('Export CSV')).toBeInTheDocument();
            expect(screen.queryByText('Rename Table')).not.toBeInTheDocument();
            expect(screen.queryByText('Delete Table')).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onImportCsv when Import CSV is clicked', async () => {
            const user = userEvent.setup();
            const onImportCsv = vi.fn();

            render(<DataTableActionsMenu {...defaultProps} onImportCsv={onImportCsv} />);

            const triggerButton = screen.getByRole('button', {name: 'More actions'});

            await user.click(triggerButton);

            const importItem = screen.getByText('Import CSV');

            await user.click(importItem);

            expect(onImportCsv).toHaveBeenCalledTimes(1);
        });

        it('should call onExportCsv when Export CSV is clicked', async () => {
            const user = userEvent.setup();
            const onExportCsv = vi.fn();

            render(<DataTableActionsMenu {...defaultProps} onExportCsv={onExportCsv} />);

            const triggerButton = screen.getByRole('button', {name: 'More actions'});

            await user.click(triggerButton);

            const exportItem = screen.getByText('Export CSV');

            await user.click(exportItem);

            expect(onExportCsv).toHaveBeenCalledTimes(1);
        });

        it('should call onRenameTable when Rename table is clicked', async () => {
            const user = userEvent.setup();
            const onRenameTable = vi.fn();

            render(<DataTableActionsMenu {...defaultProps} onRenameTable={onRenameTable} />);

            const triggerButton = screen.getByRole('button', {name: 'More actions'});

            await user.click(triggerButton);

            const renameItem = screen.getByText('Rename Table');

            await user.click(renameItem);

            expect(onRenameTable).toHaveBeenCalledTimes(1);
        });

        it('should call onDeleteTable when Delete table is clicked', async () => {
            const user = userEvent.setup();
            const onDeleteTable = vi.fn();

            render(<DataTableActionsMenu {...defaultProps} onDeleteTable={onDeleteTable} />);

            const triggerButton = screen.getByRole('button', {name: 'More actions'});

            await user.click(triggerButton);

            const deleteItem = screen.getByText('Delete Table');

            await user.click(deleteItem);

            expect(onDeleteTable).toHaveBeenCalledTimes(1);
        });
    });
});
