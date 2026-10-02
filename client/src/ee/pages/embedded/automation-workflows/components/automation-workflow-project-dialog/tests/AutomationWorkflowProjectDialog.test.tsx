import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectDialog from '../AutomationWorkflowProjectDialog';

const existingProject = {
    categoryId: null,
    description: null,
    id: '1',
    lastPublishedVersion: null,
    name: 'Existing',
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [],
};

const onClose = vi.fn();
const onSubmit = vi.fn();

const defaultProps = {
    categories: [{id: '1', name: 'Marketing'}],
    onClose,
    onSubmit,
    tags: [{id: '1', name: 'crm'}],
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AutomationWorkflowProjectDialog', () => {
    describe('create mode', () => {
        it('should render the create title and description', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByText('Create Project')).toBeInTheDocument();
            expect(screen.getByText('Use this to create a project which will contain workflows')).toBeInTheDocument();
        });

        it('should render empty name and description fields', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByLabelText('Name')).toHaveValue('');
            expect(screen.getByLabelText('Description')).toHaveValue('');
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and description', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} project={existingProject} />);

            expect(screen.getByText('Edit Project')).toBeInTheDocument();
            expect(screen.getByText('Use this to edit a project which will contain workflows')).toBeInTheDocument();
        });

        it('should prefill the name from the project', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} project={existingProject} />);

            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should render the category and tags fields', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByText('Category')).toBeInTheDocument();
            expect(screen.getByText('Tags')).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should submit the entered values', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.type(screen.getByLabelText('Name'), 'New project');
            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({name: 'New project'}));
        });
    });
});
