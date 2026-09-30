import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {Trash2Icon} from 'lucide-react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteAlertDialog from '../DeleteAlertDialog';

const onCancel = vi.fn();
const onDelete = vi.fn();

const defaultProps = {
    onCancel,
    onDelete,
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('DeleteAlertDialog', () => {
    it('should render nothing when open is false', () => {
        render(<DeleteAlertDialog {...defaultProps} open={false} />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('should render an alertdialog when open is true', () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });
});

describe('DeleteAlertDialog default copy', () => {
    it('should render the generic title and description', () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Are you absolutely sure?');
        expect(
            screen.getByText('This action cannot be undone. This will permanently delete data.')
        ).toBeInTheDocument();
    });

    it('should label the footer buttons Cancel and Delete', () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Keep node'})).not.toBeInTheDocument();
    });

    it('should render the Delete button without an icon', () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete'}).querySelector('svg')).toBeNull();
    });
});

describe('DeleteAlertDialog node copy', () => {
    const nodeProps = {...defaultProps, nodeName: 'httpClient_1'};

    it('should name the node in the title', () => {
        render(<DeleteAlertDialog {...nodeProps} />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Delete node httpClient_1?');
    });

    it('should describe what deleting a node removes', () => {
        render(<DeleteAlertDialog {...nodeProps} />);

        expect(
            screen.getByText(
                'This action cannot be undone. This will permanently delete the node and properties it contains.'
            )
        ).toBeInTheDocument();
    });

    it('should label the footer buttons Keep node and Delete node', () => {
        render(<DeleteAlertDialog {...nodeProps} />);

        expect(screen.getByRole('button', {name: 'Keep node'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete node'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
    });

    it('should render the Delete node button with an icon', () => {
        render(<DeleteAlertDialog {...nodeProps} />);

        expect(screen.getByRole('button', {name: 'Delete node'}).querySelector('svg')).toBeInTheDocument();
    });
});

describe('DeleteAlertDialog dismissal', () => {
    it('should call onCancel when the Cancel button is clicked', async () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onDelete).not.toHaveBeenCalled();
    });

    it('should call onCancel when the close button is clicked', async () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onDelete).not.toHaveBeenCalled();
    });

    it('should call onCancel when Escape is pressed', async () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        await userEvent.keyboard('{Escape}');

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onDelete).not.toHaveBeenCalled();
    });
});

describe('DeleteAlertDialog confirmation', () => {
    it('should call onDelete when the Delete button is clicked', async () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onDelete).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });

    it('should call onDelete when the Delete node button is clicked', async () => {
        render(<DeleteAlertDialog {...defaultProps} nodeName="httpClient_1" />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete node'}));

        expect(onDelete).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });

    it('should give the confirming action the destructive treatment', () => {
        render(<DeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete'})).toHaveClass('bg-surface-destructive-primary');
    });
});

describe('DeleteAlertDialog overrides', () => {
    it('should use confirmLabel in place of Delete', () => {
        render(<DeleteAlertDialog {...defaultProps} confirmLabel="Remove" />);

        expect(screen.getByRole('button', {name: 'Remove'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete'})).not.toBeInTheDocument();
    });

    it('should use cancelLabel in place of Cancel', () => {
        render(<DeleteAlertDialog {...defaultProps} cancelLabel="Keep it" />);

        expect(screen.getByRole('button', {name: 'Keep it'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
    });

    it('should render confirmIcon on the confirming action', () => {
        render(<DeleteAlertDialog {...defaultProps} confirmIcon={<Trash2Icon data-testid="custom-icon" />} />);

        expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
    });

    it('should still call onDelete through a relabelled confirming action', async () => {
        render(<DeleteAlertDialog {...defaultProps} confirmLabel="Remove" />);

        await userEvent.click(screen.getByRole('button', {name: 'Remove'}));

        expect(onDelete).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });
});

describe('DeleteAlertDialog override precedence over the nodeName preset', () => {
    const nodeProps = {...defaultProps, nodeName: 'httpClient_1'};

    it('should let confirmLabel win over Delete node', () => {
        render(<DeleteAlertDialog {...nodeProps} confirmLabel="Remove node" />);

        expect(screen.getByRole('button', {name: 'Remove node'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete node'})).not.toBeInTheDocument();
    });

    it('should let cancelLabel win over Keep node', () => {
        render(<DeleteAlertDialog {...nodeProps} cancelLabel="Never mind" />);

        expect(screen.getByRole('button', {name: 'Never mind'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Keep node'})).not.toBeInTheDocument();
    });

    it('should keep the title and description driven by nodeName when labels are overridden', () => {
        render(<DeleteAlertDialog {...nodeProps} cancelLabel="Never mind" confirmLabel="Remove node" />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Delete node httpClient_1?');
        expect(
            screen.getByText(
                'This action cannot be undone. This will permanently delete the node and properties it contains.'
            )
        ).toBeInTheDocument();
    });
});
