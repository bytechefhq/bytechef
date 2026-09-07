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

interface EditDialogHarnessProps {
    currentInputIndex?: number;
    defaultValues: WorkflowInputType;
    saveWorkflowInput?: (input: WorkflowInputType) => void;
}

const EditDialogHarness = ({
    currentInputIndex = 0,
    defaultValues,
    saveWorkflowInput = vi.fn(),
}: EditDialogHarnessProps) => {
    const nameInputRef = useRef<HTMLInputElement>(null);

    const form = useForm<WorkflowInputType, unknown, WorkflowInputType>({defaultValues});

    return (
        <WorkflowInputsEditDialog
            closeDialog={vi.fn()}
            currentInputIndex={currentInputIndex}
            form={form}
            isEditDialogOpen={true}
            nameInputRef={nameInputRef}
            openEditDialog={vi.fn()}
            saveWorkflowInput={saveWorkflowInput}
        />
    );
};

const newInputDefaultValues = (name: string): WorkflowInputType => ({
    label: 'Label',
    name,
    required: false,
    testValue: '',
    type: 'string',
});

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

    it.each(['my input', 'my-input', '1input', 'my.input', 'my@input'])(
        'should reject the name %s, which the expression evaluator cannot resolve',
        async (name) => {
            const saveWorkflowInput = vi.fn();

            render(
                <EditDialogHarness
                    currentInputIndex={-1}
                    defaultValues={newInputDefaultValues(name)}
                    saveWorkflowInput={saveWorkflowInput}
                />
            );

            fireEvent.click(await screen.findByRole('button', {name: 'Save'}));

            expect(
                await screen.findByText(
                    'Name must start with a letter or underscore and contain only letters, digits and underscores'
                )
            ).toBeInTheDocument();

            expect(saveWorkflowInput).not.toHaveBeenCalled();
        }
    );

    it.each(['myInput', 'my_input', '_leading', 'input1', 'INPUT_2'])('should accept the name %s', async (name) => {
        const saveWorkflowInput = vi.fn();

        render(
            <EditDialogHarness
                currentInputIndex={-1}
                defaultValues={newInputDefaultValues(name)}
                saveWorkflowInput={saveWorkflowInput}
            />
        );

        fireEvent.click(await screen.findByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInput).toHaveBeenCalled());
    });
});
