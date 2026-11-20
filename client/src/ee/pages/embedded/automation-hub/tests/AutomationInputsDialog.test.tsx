import AutomationInputsDialog from '@/ee/pages/embedded/automation-hub/views/components/AutomationInputsDialog';
import {AutomationProjectWorkflowInput} from '@/ee/shared/middleware/embedded/public';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {updateInputsMutateAsyncMock} = vi.hoisted(() => ({
    updateInputsMutateAsyncMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations', () => ({
    useUpdateAutomationInputsMutation: () => ({isPending: false, mutateAsync: updateInputsMutateAsyncMock}),
}));

const INPUTS: AutomationProjectWorkflowInput[] = [
    {label: 'Spreadsheet name', name: 'sheetName', required: true, type: 'STRING'},
    {label: 'API region', name: 'region', required: true, type: 'STRING'},
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

    it('shows every input as an editable field', () => {
        renderDialog();

        expect(screen.queryByText('Provided by your administrator')).not.toBeInTheDocument();
        expect(screen.getByLabelText(/API region/)).toBeInTheDocument();
        expect(screen.getByLabelText(/Spreadsheet name/)).toBeInTheDocument();
    });

    it('keeps Save disabled until every required input has a value and then sends every value', async () => {
        const user = userEvent.setup();

        renderDialog();

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).toBeDisabled();

        await user.type(screen.getByLabelText(/API region/), 'eu');

        expect(saveButton).toBeEnabled();

        await user.click(saveButton);

        await waitFor(() =>
            expect(updateInputsMutateAsyncMock).toHaveBeenCalledWith({
                inputs: {region: 'eu', sheetName: 'Leads'},
                workflowUuid: 'copy-uuid',
            })
        );
    });

    it('disables Save when the workflow has no inputs', () => {
        render(
            <AutomationInputsDialog
                inputValues={{}}
                inputs={[]}
                label="Sync leads"
                onClose={vi.fn()}
                workflowUuid="copy-uuid"
            />
        );

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });
});
