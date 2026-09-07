import {WorkflowInputType} from '@/shared/types';
import {
    fireEvent,
    mockScrollIntoView,
    render,
    resetAll,
    screen,
    waitFor,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {useRef} from 'react';
import {useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowInputsEditDialog from './WorkflowInputsEditDialog';

const EditDialogHarness = ({defaultValues}: {defaultValues: WorkflowInputType}) => {
    const nameInputRef = useRef<HTMLInputElement>(null);

    const form = useForm<WorkflowInputType, unknown, WorkflowInputType>({defaultValues});

    return (
        <WorkflowInputsEditDialog
            closeDialog={vi.fn()}
            currentInputIndex={0}
            form={form}
            isEditDialogOpen={true}
            nameInputRef={nameInputRef}
            openEditDialog={vi.fn()}
            saveWorkflowInput={vi.fn()}
        />
    );
};

beforeEach(() => {
    windowResizeObserver();

    mockScrollIntoView();
});

afterEach(() => {
    resetAll();
});

describe('WorkflowInputsEditDialog', () => {
    it('should keep the existing test value when an input is opened for editing', async () => {
        render(
            <EditDialogHarness
                defaultValues={{
                    label: 'Long Input',
                    name: 'longInput',
                    required: false,
                    testValue: 'a-very-long-test-value-that-overflows-the-workflow-inputs-panel',
                    type: 'string',
                }}
            />
        );

        expect(await screen.findByLabelText('Test Value')).toHaveValue(
            'a-very-long-test-value-that-overflows-the-workflow-inputs-panel'
        );
    });

    it('should clear the test value when the input type changes', async () => {
        render(
            <EditDialogHarness
                defaultValues={{
                    label: 'Long Input',
                    name: 'longInput',
                    required: false,
                    testValue: '1234',
                    type: 'string',
                }}
            />
        );

        const [typeSelectTrigger] = await screen.findAllByRole('combobox');

        fireEvent.click(typeSelectTrigger);

        await waitFor(() => expect(screen.getByRole('option', {name: 'Number'})).toBeInTheDocument());

        fireEvent.click(screen.getByRole('option', {name: 'Number'}));

        await waitFor(() => expect(screen.getByLabelText('Test Value')).toHaveValue(null));
    });
});
