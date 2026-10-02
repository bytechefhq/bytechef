import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApprovalTaskCreateDialog from '../ApprovalTaskCreateDialog';

const hoisted = vi.hoisted(() => ({
    handleCloseDialog: vi.fn(),
    handleOpenChange: vi.fn(),
    handleSubmit: vi.fn(),
    state: {isOpen: true},
}));

vi.mock('../hooks/useApprovalTaskCreateDialog', () => ({
    useApprovalTaskCreateDialog: () => ({
        availableAssigneeOptions: [],
        errors: {},
        form: {assignees: [], description: '', dueDate: undefined, priority: 'MEDIUM', title: ''},
        handleAssigneeChange: vi.fn(),
        handleCloseDialog: hoisted.handleCloseDialog,
        handleFormChange: vi.fn(),
        handleOpenChange: hoisted.handleOpenChange,
        handleOpenDialog: vi.fn(),
        handleSubmit: hoisted.handleSubmit,
        isOpen: hoisted.state.isOpen,
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.state.isOpen = true;
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ApprovalTaskCreateDialog', () => {
    describe('rendering', () => {
        it('should render the title and description', () => {
            render(<ApprovalTaskCreateDialog />);

            expect(screen.getByRole('heading', {name: 'Create New Approval Task'})).toBeInTheDocument();
            expect(
                screen.getByText('Add a new approval task to your project. Fill in the details below to get started.')
            ).toBeInTheDocument();
        });

        it('should render the default trigger while the dialog is closed', () => {
            hoisted.state.isOpen = false;

            render(<ApprovalTaskCreateDialog />);

            expect(screen.getByRole('button', {name: 'New Approval Task'})).toBeInTheDocument();
        });

        it('should render the close, cancel and create controls', () => {
            render(<ApprovalTaskCreateDialog />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Create Approval Task'})).toBeInTheDocument();
        });

        it('should not render the dialog when closed', () => {
            hoisted.state.isOpen = false;

            render(<ApprovalTaskCreateDialog />);

            expect(screen.queryByRole('heading', {name: 'Create New Approval Task'})).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should close the dialog when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ApprovalTaskCreateDialog />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(hoisted.handleCloseDialog).toHaveBeenCalledTimes(1);
        });

        it('should close the dialog when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ApprovalTaskCreateDialog />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(hoisted.handleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should submit when the create action is clicked', async () => {
            const user = userEvent.setup();

            render(<ApprovalTaskCreateDialog />);

            await user.click(screen.getByRole('button', {name: 'Create Approval Task'}));

            expect(hoisted.handleSubmit).toHaveBeenCalledTimes(1);
        });
    });
});
