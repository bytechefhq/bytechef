import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {describe, expect, it, vi} from 'vitest';

const workflow = {definition: '{}', id: 'workflow-1', label: 'My Workflow', version: 1} as Workflow;

const useGetWorkflowQuery = () => ({data: workflow}) as UseQueryResult<Workflow, Error>;

const renderDialog = (saveDisabled: boolean) => {
    const mutate = vi.fn();
    const onSave = vi.fn();

    render(
        <WorkflowDialog
            onSave={onSave}
            saveDisabled={saveDisabled}
            updateWorkflowMutation={{isPending: false, mutate} as unknown as UseMutationResult}
            useGetWorkflowQuery={useGetWorkflowQuery}
            workflowId="workflow-1"
        />
    );

    return {mutate, onSave};
};

describe('WorkflowDialog', () => {
    it('neither saves from the button nor from Enter while saving is disabled', async () => {
        const {mutate, onSave} = renderDialog(true);

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.type(screen.getByLabelText('Label'), '{Enter}');

        expect(mutate).not.toHaveBeenCalled();
        expect(onSave).not.toHaveBeenCalled();
    });

    it('saves on Enter when saving is allowed', async () => {
        const {mutate, onSave} = renderDialog(false);

        expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled();

        await userEvent.type(screen.getByLabelText('Label'), '{Enter}');

        expect(mutate).toHaveBeenCalled();
        expect(onSave).toHaveBeenCalled();
    });
});
