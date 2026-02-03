import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import UnsavedChangesAlertDialog from '../UnsavedChangesAlertDialog';

describe('UnsavedChangesAlertDialog', () => {
    const defaultProps = {
        onCancel: vi.fn(),
        onClose: vi.fn(),
        open: true,
    };

    it('renders the dialog when open is true', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
        expect(screen.getByText('There are unsaved changes. This action cannot be undone.')).toBeInTheDocument();
    });

    it('does not render the dialog when open is false', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} open={false} />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('renders Cancel and Close buttons', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
    });

    it('calls onCancel when Cancel button is clicked', () => {
        const onCancelMock = vi.fn();

        render(<UnsavedChangesAlertDialog {...defaultProps} onCancel={onCancelMock} />);

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onCancelMock).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when Close button is clicked', () => {
        const onCloseMock = vi.fn();

        render(<UnsavedChangesAlertDialog {...defaultProps} onClose={onCloseMock} />);

        fireEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(onCloseMock).toHaveBeenCalledTimes(1);
    });

    it('Cancel button has shadow-none class', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});

        expect(cancelButton).toHaveClass('shadow-none');
    });

    it('displays correct title and description', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        const title = screen.getByRole('heading', {level: 2});

        expect(title).toHaveTextContent('Are you absolutely sure?');

        const description = screen.getByText('There are unsaved changes. This action cannot be undone.');

        expect(description).toBeInTheDocument();
    });
});
