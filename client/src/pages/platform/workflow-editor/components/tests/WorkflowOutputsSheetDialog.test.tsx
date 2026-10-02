import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowOutputsSheetDialog from '../WorkflowOutputsSheetDialog';

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock(
    '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput',
    () => ({
        default: () => <div data-testid="mentions-input" />,
    })
);

const onClose = vi.fn();

const workflow = {
    definition: JSON.stringify({outputs: [{name: 'existingOutput', value: '${trigger_1.body}'}]}),
    id: 'w1',
    label: 'My Workflow',
    outputs: [{name: 'existingOutput', value: '${trigger_1.body}'}],
} as unknown as Workflow;

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkflowOutputsSheetDialog', () => {
    describe('create mode', () => {
        it('should render the create title and description', () => {
            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={-1} workflow={workflow} />);

            expect(screen.getByRole('heading', {name: 'Create Workflow Output'})).toBeInTheDocument();
            expect(screen.getByText('Create a new workflow output expression.')).toBeInTheDocument();
        });

        it('should leave the name editable', () => {
            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={-1} workflow={workflow} />);

            expect(screen.getByPlaceholderText('Add new output name')).not.toHaveAttribute('readonly');
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and description', () => {
            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={0} workflow={workflow} />);

            expect(screen.getByRole('heading', {name: 'Edit Workflow Output'})).toBeInTheDocument();
            expect(screen.getByText('Edit the workflow output expression.')).toBeInTheDocument();
        });

        it('should lock the name of an existing output', () => {
            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={0} workflow={workflow} />);

            expect(screen.getByPlaceholderText('Add new output name')).toHaveAttribute('readonly');
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={-1} workflow={workflow} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={-1} workflow={workflow} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should close the dialog when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={-1} workflow={workflow} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(screen.queryByRole('heading', {name: 'Create Workflow Output'})).not.toBeInTheDocument();
        });

        it('should close the dialog when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<WorkflowOutputsSheetDialog onClose={onClose} outputIndex={-1} workflow={workflow} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(screen.queryByRole('heading', {name: 'Create Workflow Output'})).not.toBeInTheDocument();
        });
    });
});
