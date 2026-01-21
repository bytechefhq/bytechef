import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {StringEditCell, createStringEditCellRenderer} from '../StringCellRenderer';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('StringEditCell', () => {
    const mockOnRowChange = vi.fn();

    describe('rendering', () => {
        it('should render the edit button trigger', () => {
            render(<StringEditCell columnName="name" onRowChange={mockOnRowChange} row={{id: '1', name: 'Test'}} />);

            expect(screen.getByRole('button', {name: /Edit text/i})).toBeInTheDocument();
        });

        it('should show popover with textarea when opened', () => {
            render(
                <StringEditCell
                    columnName="description"
                    onRowChange={mockOnRowChange}
                    row={{description: 'Hello', id: '1'}}
                />
            );

            // Popover is open by default
            expect(screen.getByRole('textbox')).toBeInTheDocument();
        });

        it('should display current value in textarea', () => {
            render(
                <StringEditCell
                    columnName="content"
                    onRowChange={mockOnRowChange}
                    row={{content: 'Current content', id: '1'}}
                />
            );

            expect(screen.getByRole('textbox')).toHaveValue('Current content');
        });

        it('should display empty string for null value', () => {
            render(<StringEditCell columnName="notes" onRowChange={mockOnRowChange} row={{id: '1', notes: null}} />);

            expect(screen.getByRole('textbox')).toHaveValue('');
        });

        it('should display empty string for undefined value', () => {
            render(<StringEditCell columnName="notes" onRowChange={mockOnRowChange} row={{id: '1'}} />);

            expect(screen.getByRole('textbox')).toHaveValue('');
        });

        it('should have placeholder text with column name', () => {
            render(<StringEditCell columnName="description" onRowChange={mockOnRowChange} row={{id: '1'}} />);

            expect(screen.getByPlaceholderText('Enter description')).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should update text state on typing', async () => {
            const user = userEvent.setup();

            render(<StringEditCell columnName="name" onRowChange={mockOnRowChange} row={{id: '1', name: ''}} />);

            const textarea = screen.getByRole('textbox');

            await user.type(textarea, 'New text');

            expect(textarea).toHaveValue('New text');
        });

        it('should call onRowChange with new value on Enter', async () => {
            const user = userEvent.setup();

            render(<StringEditCell columnName="name" onRowChange={mockOnRowChange} row={{id: '1', name: ''}} />);

            const textarea = screen.getByRole('textbox');

            await user.type(textarea, 'Saved value');
            await user.keyboard('{Enter}');

            expect(mockOnRowChange).toHaveBeenCalledWith({id: '1', name: 'Saved value'}, true);
        });

        it('should allow shift+enter for newlines', async () => {
            const user = userEvent.setup();

            render(<StringEditCell columnName="name" onRowChange={mockOnRowChange} row={{id: '1', name: ''}} />);

            const textarea = screen.getByRole('textbox');

            await user.type(textarea, 'Line 1{Shift>}{Enter}{/Shift}Line 2');

            expect(textarea).toHaveValue('Line 1\nLine 2');
        });

        it('should call onRowChange with original value on Escape', async () => {
            const user = userEvent.setup();

            render(
                <StringEditCell columnName="name" onRowChange={mockOnRowChange} row={{id: '1', name: 'Original'}} />
            );

            const textarea = screen.getByRole('textbox');

            await user.clear(textarea);
            await user.type(textarea, 'Changed');
            await user.keyboard('{Escape}');

            expect(mockOnRowChange).toHaveBeenLastCalledWith({id: '1', name: 'Original'}, true);
        });
    });

    describe('createStringEditCellRenderer factory', () => {
        it('should create a renderer that accepts row props', () => {
            const StringRenderer = createStringEditCellRenderer('title');

            render(<StringRenderer onRowChange={mockOnRowChange} row={{id: '1', title: 'Test Title'}} />);

            expect(screen.getByRole('textbox')).toHaveValue('Test Title');
        });
    });
});
