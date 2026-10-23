import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {FieldValues, useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import ProvisionIntegrationDialogWorkflowItem from '../ProvisionIntegrationDialogWorkflowItem';

const WorkflowItemHarness = ({onValid}: {onValid: (values: FieldValues) => void}) => {
    const {control, formState, handleSubmit} = useForm<FieldValues>({
        defaultValues: {workflows: [{enabled: true, inputs: {}, workflowUuid: 'u-1'}]},
    });

    return (
        <>
            <ProvisionIntegrationDialogWorkflowItem
                control={control}
                formState={formState}
                onEnabledChange={vi.fn()}
                workflow={{
                    id: 'wf-1',
                    inputs: [{label: 'Channel', name: 'channel', required: true, type: 'string'}],
                    label: 'Notify',
                }}
                workflowIndex={0}
            />

            <button onClick={handleSubmit(onValid)} type="button">
                Submit
            </button>
        </>
    );
};

describe('ProvisionIntegrationDialogWorkflowItem', () => {
    it('registers a required rule for a required workflow input', async () => {
        const onValid = vi.fn();

        render(
            <QueryClientProvider client={new QueryClient()}>
                <WorkflowItemHarness onValid={onValid} />
            </QueryClientProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Submit'}));

        await waitFor(() => expect(screen.getByText('This field is required.')).toBeInTheDocument());

        expect(onValid).not.toHaveBeenCalled();
    });
});
