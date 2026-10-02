import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowInputsSheetContent from './WorkflowInputsSheetContent';

const workflowInput = {label: 'Customer ID', name: 'customerId', required: true, type: 'string'};

const hoisted = vi.hoisted(() => ({
    closeDeleteDialogMock: vi.fn(),
    currentInputIndex: 0,
    deleteWorkflowInputMock: vi.fn(),
}));

vi.mock('./hooks/useWorkflowInputs', () => ({
    default: () => ({
        closeDeleteDialog: hoisted.closeDeleteDialogMock,
        closeEditDialog: vi.fn(),
        currentInputIndex: hoisted.currentInputIndex,
        deleteWorkflowInput: hoisted.deleteWorkflowInputMock,
        form: {},
        isDeleteDialogOpen: true,
        isDeletePending: false,
        isEditDialogOpen: false,
        nameInputRef: {current: null},
        openDeleteDialog: vi.fn(),
        openEditDialog: vi.fn(),
        saveWorkflowInput: vi.fn(),
        workflow: {inputs: [workflowInput]},
    }),
}));

vi.mock('./WorkflowInputsTable', () => ({
    default: () => null,
}));

vi.mock('./WorkflowInputsEditDialog', () => ({
    default: () => null,
}));

vi.mock('@/components/ui/sheet', () => ({
    SheetCloseButton: () => null,
}));

const renderContent = () => render(<WorkflowInputsSheetContent invalidateWorkflowQueries={vi.fn()} />);

beforeEach(() => {
    windowResizeObserver();

    hoisted.currentInputIndex = 0;
});

afterEach(() => {
    resetAll();
});

describe('WorkflowInputsSheetContent delete dialog', () => {
    it('deletes the selected input when confirmed', async () => {
        renderContent();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteWorkflowInputMock).toHaveBeenCalledWith(workflowInput);
    });

    it('does not delete anything when the selected input no longer exists', async () => {
        hoisted.currentInputIndex = 5;

        renderContent();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteWorkflowInputMock).not.toHaveBeenCalled();
    });

    it('closes the dialog when cancelled', async () => {
        renderContent();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.closeDeleteDialogMock).toHaveBeenCalledTimes(1);
        expect(hoisted.deleteWorkflowInputMock).not.toHaveBeenCalled();
    });
});
