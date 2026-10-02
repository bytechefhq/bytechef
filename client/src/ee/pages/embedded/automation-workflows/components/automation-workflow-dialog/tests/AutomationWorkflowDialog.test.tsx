import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowDialog from '../AutomationWorkflowDialog';

const onClose = vi.fn();
const onSubmit = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AutomationWorkflowDialog', () => {
    describe('create mode', () => {
        it('should render the create title and description', () => {
            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            expect(screen.getByText('Create Workflow')).toBeInTheDocument();
            expect(screen.getByText('Create a new workflow by filling out the form below.')).toBeInTheDocument();
        });

        it('should render empty fields', () => {
            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            expect(screen.getByLabelText('Label')).toHaveValue('');
            expect(screen.getByLabelText('Description')).toHaveValue('');
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and description', () => {
            render(
                <AutomationWorkflowDialog
                    onClose={onClose}
                    onSubmit={onSubmit}
                    workflow={{description: 'Existing description', label: 'Existing label'}}
                />
            );

            expect(screen.getByText('Edit Workflow')).toBeInTheDocument();
            expect(screen.getByText("Update the workflow's label and description.")).toBeInTheDocument();
        });

        it('should prefill the fields from the workflow', () => {
            render(
                <AutomationWorkflowDialog
                    onClose={onClose}
                    onSubmit={onSubmit}
                    workflow={{description: 'Existing description', label: 'Existing label'}}
                />
            );

            expect(screen.getByLabelText('Label')).toHaveValue('Existing label');
            expect(screen.getByLabelText('Description')).toHaveValue('Existing description');
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should submit the entered values', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            await user.type(screen.getByLabelText('Label'), 'New workflow');
            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({label: 'New workflow'}));
        });

        it('should not submit without a label', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowDialog onClose={onClose} onSubmit={onSubmit} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSubmit).not.toHaveBeenCalled();
        });
    });
});
