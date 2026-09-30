import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {Trash2Icon} from 'lucide-react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AlertDialog from '../AlertDialog';

const onCancel = vi.fn();
const onConfirm = vi.fn();

const defaultProps = {
    onCancel,
    onConfirm,
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AlertDialog', () => {
    it('should render nothing when open is false', () => {
        render(<AlertDialog {...defaultProps} open={false} />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('should render an alertdialog when open is true', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });
});

describe('AlertDialog default copy', () => {
    it('should render the generic title and description', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Are you absolutely sure?');
        expect(
            screen.getByText('This action cannot be undone. This will permanently delete data.')
        ).toBeInTheDocument();
    });

    it('should label the footer buttons Cancel and Delete', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should render the Delete button without an icon', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete'}).querySelector('svg')).toBeNull();
    });
});

describe('AlertDialog dismissal', () => {
    it('should call onCancel when the Cancel button is clicked', async () => {
        render(<AlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onConfirm).not.toHaveBeenCalled();
    });

    it('should call onCancel when the close button is clicked', async () => {
        render(<AlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onConfirm).not.toHaveBeenCalled();
    });

    it('should call onCancel when Escape is pressed', async () => {
        render(<AlertDialog {...defaultProps} />);

        await userEvent.keyboard('{Escape}');

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onConfirm).not.toHaveBeenCalled();
    });
});

describe('AlertDialog confirmation', () => {
    it('should call onConfirm when the Delete button is clicked', async () => {
        render(<AlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onConfirm).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });

    it('should give the confirming action the destructive treatment', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete'})).toHaveClass('bg-surface-destructive-primary');
    });
});

describe('AlertDialog alert semantics', () => {
    it('should keep the alertdialog role rather than a plain dialog', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('should focus the close button on open', async () => {
        render(<AlertDialog {...defaultProps} />);

        await waitFor(() => expect(screen.getByRole('button', {name: 'Close'})).toHaveFocus());
    });

    // There is deliberately no outside-click test here. Radix's outside dismissal never fires under jsdom — with
    // the onInteractOutside guard removed, pointerdown on the body, on the overlay and on the document all leave the
    // dialog open — so any such test would pass whether or not the guard exists. The guard is verified in a browser.

    it('should leave closing to the caller when the confirming action is clicked', async () => {
        render(<AlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onConfirm).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });
});

describe('AlertDialog overrides', () => {
    it('should use confirmLabel in place of Delete', () => {
        render(<AlertDialog {...defaultProps} confirmLabel="Remove" />);

        expect(screen.getByRole('button', {name: 'Remove'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete'})).not.toBeInTheDocument();
    });

    it('should use cancelLabel in place of Cancel', () => {
        render(<AlertDialog {...defaultProps} cancelLabel="Keep it" />);

        expect(screen.getByRole('button', {name: 'Keep it'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
    });

    it('should render confirmIcon on the confirming action', () => {
        render(<AlertDialog {...defaultProps} confirmIcon={<Trash2Icon data-testid="custom-icon" />} />);

        expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
    });

    it('should still call onConfirm through a relabelled confirming action', async () => {
        render(<AlertDialog {...defaultProps} confirmLabel="Remove" />);

        await userEvent.click(screen.getByRole('button', {name: 'Remove'}));

        expect(onConfirm).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });

    it('should use title in place of the generic question', () => {
        render(<AlertDialog {...defaultProps} title="Delete column" />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Delete column');
        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });

    it('should use description in place of the generic one', () => {
        render(<AlertDialog {...defaultProps} description="This will permanently delete the connection." />);

        expect(screen.getByRole('alertdialog')).toHaveAccessibleDescription(
            'This will permanently delete the connection.'
        );
        expect(
            screen.queryByText('This action cannot be undone. This will permanently delete data.')
        ).not.toBeInTheDocument();
    });

    it('should accept a description built from elements rather than a plain string', () => {
        render(
            <AlertDialog
                {...defaultProps}
                description={
                    <>
                        This will permanently remove <strong>notes.md</strong> from the skill.
                    </>
                }
            />
        );

        expect(screen.getByRole('alertdialog')).toHaveAccessibleDescription(
            'This will permanently remove notes.md from the skill.'
        );
    });

    it('should name the confirming action with ariaLabel for the benefit of assistive tech', () => {
        render(<AlertDialog {...defaultProps} ariaLabel="Confirm Project Deletion" />);

        expect(screen.getByRole('button', {name: 'Confirm Project Deletion'})).toBeInTheDocument();
    });

    it('should leave the confirming action named by its label when no ariaLabel is given', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete'})).not.toHaveAttribute('aria-label');
    });
});

describe('AlertDialog pending state', () => {
    it('should disable the confirming action while the deletion is in flight', () => {
        render(<AlertDialog {...defaultProps} isPending />);

        expect(screen.getByRole('button', {name: 'Delete'})).toBeDisabled();
    });

    it('should not call onConfirm while the deletion is in flight', async () => {
        render(<AlertDialog {...defaultProps} isPending />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}), {pointerEventsCheck: 0});

        expect(onConfirm).not.toHaveBeenCalled();
    });

    it('should show a loading icon in place of the confirm icon while pending', () => {
        render(<AlertDialog {...defaultProps} confirmIcon={<Trash2Icon data-testid="custom-icon" />} isPending />);

        expect(screen.queryByTestId('custom-icon')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete'}).querySelector('svg')).toBeInTheDocument();
    });

    it('should enable the confirming action when it is not pending', () => {
        render(<AlertDialog {...defaultProps} isPending={false} />);

        expect(screen.getByRole('button', {name: 'Delete'})).toBeEnabled();
    });
});

describe('AlertDialog confirm variant', () => {
    it('should give the confirming action the destructive treatment by default', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete'})).toHaveClass('bg-surface-destructive-primary');
    });

    it('should give the confirming action the brand treatment when confirmButtonVariant is default', () => {
        render(<AlertDialog {...defaultProps} confirmButtonVariant="default" confirmLabel="Confirm" />);

        const confirmButton = screen.getByRole('button', {name: 'Confirm'});

        expect(confirmButton).toHaveClass('bg-surface-brand-primary');
        expect(confirmButton).not.toHaveClass('bg-surface-destructive-primary');
    });

    it('should give the confirming action the ghost treatment when confirmButtonVariant is destructiveGhost', () => {
        render(
            <AlertDialog {...defaultProps} confirmButtonVariant="destructiveGhost" confirmLabel="Close & discard" />
        );

        const confirmButton = screen.getByRole('button', {name: 'Close & discard'});

        expect(confirmButton).toHaveClass('text-content-destructive-primary');
        expect(confirmButton).not.toHaveClass('bg-surface-destructive-primary');
    });

    it('should merge confirmClassName onto the confirming action', () => {
        render(
            <AlertDialog {...defaultProps} confirmButtonVariant="destructiveGhost" confirmClassName="opacity-100" />
        );

        expect(screen.getByRole('button', {name: 'Delete'})).toHaveClass('opacity-100');
    });

    it('should still call onConfirm through a non-destructive confirming action', async () => {
        render(<AlertDialog {...defaultProps} confirmButtonVariant="default" confirmLabel="Upgrade now" />);

        await userEvent.click(screen.getByRole('button', {name: 'Upgrade now'}));

        expect(onConfirm).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });
});

describe('AlertDialog pending cancel', () => {
    it('should disable the cancel button while the confirmed action is in flight', () => {
        render(<AlertDialog {...defaultProps} isPending />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeDisabled();
    });

    it('should leave the cancel button enabled when nothing is in flight', () => {
        render(<AlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeEnabled();
    });
});
