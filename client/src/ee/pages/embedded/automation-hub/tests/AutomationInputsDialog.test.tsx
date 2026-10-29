import AutomationInputsDialog from '@/ee/pages/embedded/automation-hub/views/components/AutomationInputsDialog';
import {AutomationWorkflowProjectWorkflowInput} from '@/ee/shared/middleware/embedded/public';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {updateInputsMutateAsyncMock} = vi.hoisted(() => ({
    updateInputsMutateAsyncMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations', () => ({
    useUpdateAutomationInputsMutation: () => ({isPending: false, mutateAsync: updateInputsMutateAsyncMock}),
}));

const INPUTS: AutomationWorkflowProjectWorkflowInput[] = [
    {label: 'Spreadsheet name', name: 'sheetName', required: true, type: 'STRING'},
    {label: 'API region', name: 'region', provided: true, required: true, type: 'STRING'},
];

const renderDialog = (onClose = vi.fn()) =>
    render(
        <AutomationInputsDialog
            inputValues={{sheetName: 'Leads'}}
            inputs={INPUTS}
            label="Sync leads"
            onClose={onClose}
            workflowUuid="copy-uuid"
        />
    );

describe('AutomationInputsDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        updateInputsMutateAsyncMock.mockResolvedValue(undefined);
    });

    it('shows a provided input as read-only with the administrator label and no field', () => {
        renderDialog();

        expect(screen.getByText('Provided by your administrator')).toBeInTheDocument();
        expect(screen.queryByLabelText(/API region/)).not.toBeInTheDocument();
        expect(screen.getByLabelText(/Spreadsheet name/)).toBeInTheDocument();
    });

    it('does not count a provided required input as missing and never sends it', async () => {
        const user = userEvent.setup();

        renderDialog();

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).toBeEnabled();

        await user.click(saveButton);

        await waitFor(() =>
            expect(updateInputsMutateAsyncMock).toHaveBeenCalledWith({
                inputs: {sheetName: 'Leads'},
                workflowUuid: 'copy-uuid',
            })
        );
    });
});
