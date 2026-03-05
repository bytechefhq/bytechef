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
        expect(screen.getByText('Discard code changes?')).toBeInTheDocument();
        expect(
            screen.getByText('You have unsaved changes. Are you sure you want to discard them?')
        ).toBeInTheDocument();
    });

    it('does not render the dialog when open is false', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} open={false} />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('renders Keep editing and Close & discard buttons', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Keep editing'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Close & discard'})).toBeInTheDocument();
    });

    it('calls onCancel when Keep editing button is clicked', () => {
        const onCancelMock = vi.fn();

        render(<UnsavedChangesAlertDialog {...defaultProps} onCancel={onCancelMock} />);

        fireEvent.click(screen.getByRole('button', {name: 'Keep editing'}));

        expect(onCancelMock).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when Close & discard button is clicked', () => {
        const onCloseMock = vi.fn();

        render(<UnsavedChangesAlertDialog {...defaultProps} onClose={onCloseMock} />);

        fireEvent.click(screen.getByRole('button', {name: 'Close & discard'}));

        expect(onCloseMock).toHaveBeenCalledTimes(1);
    });

    it('Keep editing button has shadow-none class', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        const keepEditingButton = screen.getByRole('button', {name: 'Keep editing'});

        expect(keepEditingButton).toHaveClass('shadow-none');
    });

    it('displays correct title and description', () => {
        render(<UnsavedChangesAlertDialog {...defaultProps} />);

        const title = screen.getByRole('heading', {level: 2});

        expect(title).toHaveTextContent('Discard code changes?');

        const description = screen.getByText('You have unsaved changes. Are you sure you want to discard them?');

        expect(description).toBeInTheDocument();
    });
});
