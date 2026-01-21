import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ImportDataTableCsvDialog from '../ImportDataTableCsvDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleImport: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        mockIsPending: false,
        mockOpen: true,
    };
});

vi.mock('../../hooks/useImportDataTableCsvDialog', () => ({
    default: () => ({
        handleImport: hoisted.mockHandleImport,
        handleOpenChange: hoisted.mockHandleOpenChange,
        isPending: hoisted.mockIsPending,
        open: hoisted.mockOpen,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockOpen = true;
    hoisted.mockIsPending = false;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ImportDataTableCsvDialog', () => {
    describe('rendering', () => {
        it('should render the dialog when open is true', () => {
            render(<ImportDataTableCsvDialog />);

            expect(screen.getByRole('dialog')).toBeInTheDocument();
        });

        it('should not render the dialog when open is false', () => {
            hoisted.mockOpen = false;

            render(<ImportDataTableCsvDialog />);

            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('should render the dialog title', () => {
            render(<ImportDataTableCsvDialog />);

            expect(screen.getByText('Import CSV')).toBeInTheDocument();
        });

        it('should render the file input label', () => {
            render(<ImportDataTableCsvDialog />);

            expect(screen.getByText('CSV file')).toBeInTheDocument();
        });

        it('should render Cancel and Import buttons', () => {
            render(<ImportDataTableCsvDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Import'})).toBeInTheDocument();
        });

        it('should disable Import button when no file is selected', () => {
            render(<ImportDataTableCsvDialog />);

            const importButton = screen.getByRole('button', {name: 'Import'});

            expect(importButton).toBeDisabled();
        });
    });

    describe('pending state', () => {
        it('should show "Importing…" text when isPending is true', () => {
            hoisted.mockIsPending = true;

            render(<ImportDataTableCsvDialog />);

            expect(screen.getByRole('button', {name: 'Importing…'})).toBeInTheDocument();
        });

        it('should disable Import button when isPending is true', () => {
            hoisted.mockIsPending = true;

            render(<ImportDataTableCsvDialog />);

            const importButton = screen.getByRole('button', {name: 'Importing…'});

            expect(importButton).toBeDisabled();
        });
    });

    describe('interactions', () => {
        it('should call handleOpenChange with false when Cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ImportDataTableCsvDialog />);

            const cancelButton = screen.getByRole('button', {name: 'Cancel'});

            await user.click(cancelButton);

            expect(hoisted.mockHandleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should accept .csv files in the file input', () => {
            render(<ImportDataTableCsvDialog />);

            const fileInput = document.querySelector('input[type="file"]');

            expect(fileInput).toHaveAttribute('accept', '.csv,text/csv');
        });
    });

    describe('file selection', () => {
        it('should show file info when a file is selected', async () => {
            const user = userEvent.setup();

            render(<ImportDataTableCsvDialog />);

            const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

            const testFile = new File(['name,age\nJohn,30'], 'test.csv', {type: 'text/csv'});

            await user.upload(fileInput, testFile);

            expect(screen.getByText('test.csv')).toBeInTheDocument();
        });

        it('should enable Import button when a file is selected', async () => {
            const user = userEvent.setup();

            render(<ImportDataTableCsvDialog />);

            const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

            const testFile = new File(['name,age\nJohn,30'], 'test.csv', {type: 'text/csv'});

            await user.upload(fileInput, testFile);

            const importButton = screen.getByRole('button', {name: 'Import'});

            expect(importButton).not.toBeDisabled();
        });

        it('should call handleImport with file content when Import is clicked', async () => {
            const user = userEvent.setup();
            const csvContent = 'name,age\nJohn,30';

            // Define File.prototype.text if it doesn't exist (jsdom doesn't support it)
            const originalText = File.prototype.text;

            File.prototype.text = vi.fn().mockResolvedValue(csvContent);

            render(<ImportDataTableCsvDialog />);

            const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
            const testFile = new File([csvContent], 'test.csv', {type: 'text/csv'});

            await user.upload(fileInput, testFile);

            const importButton = screen.getByRole('button', {name: 'Import'});

            await user.click(importButton);

            // Wait for the async operation to complete
            await vi.waitFor(() => {
                expect(hoisted.mockHandleImport).toHaveBeenCalledWith(csvContent);
            });

            // Restore original
            if (originalText) {
                File.prototype.text = originalText;
            }
        });
    });
});
