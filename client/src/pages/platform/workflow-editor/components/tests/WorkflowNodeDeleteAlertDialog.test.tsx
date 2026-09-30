import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDeleteAlertDialog from '../WorkflowNodeDeleteAlertDialog';

const onCancel = vi.fn();
const onConfirm = vi.fn();

const defaultProps = {
    nodeLabel: 'httpClient_1',
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

describe('WorkflowNodeDeleteAlertDialog', () => {
    it('should name the node in the title', () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Delete node httpClient_1?');
    });

    it('should keep the node title without a name when the node has no label', () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} nodeLabel={undefined} />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Delete node?');
        expect(screen.getByRole('button', {name: 'Delete node'})).toBeInTheDocument();
    });

    it('should describe what deleting a node removes', () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} />);

        expect(
            screen.getByText(
                'This action cannot be undone. This will permanently delete the node and properties it contains.'
            )
        ).toBeInTheDocument();
    });

    it('should label the footer buttons Keep node and Delete node', () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Keep node'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete node'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
    });

    it('should render the Delete node button with an icon', () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} />);

        expect(screen.getByRole('button', {name: 'Delete node'}).querySelector('svg')).toBeInTheDocument();
    });

    it('should call onConfirm when the Delete node button is clicked', async () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete node'}));

        expect(onConfirm).toHaveBeenCalledTimes(1);
        expect(onCancel).not.toHaveBeenCalled();
    });

    it('should call onCancel when the Keep node button is clicked', async () => {
        render(<WorkflowNodeDeleteAlertDialog {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'Keep node'}));

        expect(onCancel).toHaveBeenCalledTimes(1);
        expect(onConfirm).not.toHaveBeenCalled();
    });
});
