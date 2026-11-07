import {WorkflowInputType} from '@/shared/types';
import {mockScrollIntoView, windowResizeObserver} from '@/shared/util/test-utils';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useEffect, useRef} from 'react';
import {useForm} from 'react-hook-form';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowInputsEditDialog from './WorkflowInputsEditDialog';

const {useGetComponentDefinitionQueryMock} = vi.hoisted(() => ({
    useGetComponentDefinitionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => useGetComponentDefinitionQueryMock(),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        useGetComponentDefinitionsQuery: () => ({
            data: [
                {inputsCount: 1, name: 'googleSheets', version: 1},
                {inputsCount: 1, name: 'slack', version: 2},
            ],
        }),
    }),
}));

const workflow = {
    definition: JSON.stringify({tasks: [{type: 'slack/v2/sendMessage'}], triggers: []}),
    label: 'Test',
    workflowTaskComponentNames: ['slack'],
} as never;

const saveWorkflowInputMock = vi.fn();

const setupUser = () => userEvent.setup({pointerEventsCheck: 0});

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
            internalOnlyVisible={true}
            isEditDialogOpen={true}
            nameInputRef={nameInputRef}
            openEditDialog={vi.fn()}
            saveWorkflowInput={saveWorkflowInput}
            workflow={workflow}
        />
    );
};

interface ResetOnOpenHarnessProps {
    openedInput: WorkflowInputType;
    saveWorkflowInput?: (input: WorkflowInputType) => void;
}

const ResetOnOpenHarness = ({openedInput, saveWorkflowInput = vi.fn()}: ResetOnOpenHarnessProps) => {
    const nameInputRef = useRef<HTMLInputElement>(null);

    const form = useForm<WorkflowInputType, unknown, WorkflowInputType>();

    useEffect(() => {
        form.reset(openedInput);
    }, [form, openedInput]);

    return (
        <WorkflowInputsEditDialog
            closeDialog={vi.fn()}
            currentInputIndex={0}
            form={form}
            internalOnlyVisible={true}
            isEditDialogOpen={true}
            nameInputRef={nameInputRef}
            openEditDialog={vi.fn()}
            saveWorkflowInput={saveWorkflowInput}
            workflow={workflow}
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

const Harness = ({defaultValues = {label: '', name: '', required: false}}: {defaultValues?: WorkflowInputType}) => {
    const form = useForm<WorkflowInputType>({defaultValues});
    const nameInputRef = useRef<HTMLInputElement>(null);

    return (
        <WorkflowInputsEditDialog
            closeDialog={() => {}}
            currentInputIndex={-1}
            form={form}
            internalOnlyVisible={true}
            isEditDialogOpen={true}
            nameInputRef={nameInputRef}
            openEditDialog={() => {}}
            saveWorkflowInput={saveWorkflowInputMock}
            workflow={workflow}
        />
    );
};

const EditHarness = ({defaultValues}: {defaultValues: WorkflowInputType}) => {
    const form = useForm<WorkflowInputType>({defaultValues});
    const nameInputRef = useRef<HTMLInputElement>(null);

    return (
        <WorkflowInputsEditDialog
            closeDialog={() => {}}
            currentInputIndex={0}
            form={form}
            internalOnlyVisible={true}
            isEditDialogOpen={true}
            nameInputRef={nameInputRef}
            openEditDialog={() => {}}
            saveWorkflowInput={saveWorkflowInputMock}
            workflow={workflow}
        />
    );
};

const selectOption = async (user: ReturnType<typeof userEvent.setup>, triggerText: string, optionName: string) => {
    await user.click(await screen.findByText(triggerText));

    const listbox = await screen.findByRole('listbox');

    await user.click(within(listbox).getByRole('option', {name: optionName}));
};

describe('WorkflowInputsEditDialog', () => {
    beforeAll(() => {
        Element.prototype.hasPointerCapture = vi.fn(() => false);
        Element.prototype.setPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
    });

    beforeEach(() => {
        saveWorkflowInputMock.mockClear();
        useGetComponentDefinitionQueryMock.mockReturnValue({
            data: {
                inputs: [
                    {
                        name: 'channel',
                        properties: [{controlType: 'SELECT', label: 'Channel', name: 'channelId', type: 'STRING'}],
                    },
                    {label: 'Date Range', name: 'dateRange', properties: []},
                ],
            },
        });
    });

    it('renders the create dialog title', () => {
        render(<Harness />);

        expect(screen.getByText('Create a new Input')).toBeInTheDocument();
    });

    it('reveals the Component select and the deployment note when Component property type is chosen', async () => {
        const user = setupUser();

        render(<Harness />);

        await selectOption(user, 'Select input type', 'Component property');

        expect(await screen.findByText('Select component')).toBeInTheDocument();
        expect(screen.getByText('Configured at deployment time.')).toBeInTheDocument();
        expect(screen.getByText('Test Value')).toBeInTheDocument();
    });

    it('offers only components used in the workflow that declare inputs', async () => {
        const user = setupUser();

        render(<Harness />);

        await selectOption(user, 'Select input type', 'Component property');
        await user.click(await screen.findByText('Select component'));

        const listbox = await screen.findByRole('listbox');

        expect(within(listbox).getByRole('option', {name: 'slack'})).toBeInTheDocument();
        expect(within(listbox).queryByRole('option', {name: 'googleSheets'})).not.toBeInTheDocument();
    });

    it('auto-fills name and label and stores the group reference when picking a single-property input', async () => {
        const user = setupUser();

        render(<Harness />);

        await selectOption(user, 'Select input type', 'Component property');
        await selectOption(user, 'Select component', 'slack');
        await selectOption(user, 'Select input', 'Channel');

        expect(screen.getByPlaceholderText('Input name (will be used as a dynamic value key)')).toHaveValue('channel');
        expect(screen.getByPlaceholderText('Input label')).toHaveValue('Channel');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        const savedInput = saveWorkflowInputMock.mock.calls[0][0];

        expect(savedInput.componentReference.componentName).toBe('slack');
        expect(savedInput.componentReference.componentVersion).toBe(2);
        expect(savedInput.componentReference.groupName).toBe('channel');
    });

    it('stores the group reference when picking a compound group', async () => {
        const user = setupUser();

        render(<Harness />);

        await selectOption(user, 'Select input type', 'Component property');
        await selectOption(user, 'Select component', 'slack');
        await selectOption(user, 'Select input', 'Date Range');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        const savedInput = saveWorkflowInputMock.mock.calls[0][0];

        expect(savedInput.componentReference.componentName).toBe('slack');
        expect(savedInput.componentReference.groupName).toBe('dateRange');
    });

    it('offers a Field Mapping input type', async () => {
        const user = setupUser();

        render(<Harness />);

        await user.click(screen.getByText('Select input type'));

        const listbox = await screen.findByRole('listbox');

        expect(within(listbox).getByRole('option', {name: 'Field Mapping'})).toBeInTheDocument();
    });

    it('renders a JSON editor for the field_mapping test value', async () => {
        const user = setupUser();

        render(<Harness />);

        await user.click(screen.getByText('Select input type'));

        const listbox = await screen.findByRole('listbox');

        await user.click(within(listbox).getByRole('option', {name: 'Field Mapping'}));

        expect(screen.getByText('Test Value')).toBeInTheDocument();
        expect(screen.getByTestId('field-mapping-json-editor')).toBeInTheDocument();
    });

    it('renders an Internal only checkbox', () => {
        render(<Harness />);

        expect(screen.getByText('Internal only')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', {name: /internal only/i})).toBeInTheDocument();
    });

    it('shows the Internal only checkbox checked when editing an input with internalOnly: true', () => {
        render(
            <EditHarness
                defaultValues={
                    {
                        internalOnly: true,
                        label: 'API Key',
                        name: 'apiKey',
                        required: false,
                        type: 'string',
                    } as WorkflowInputType
                }
            />
        );

        expect(screen.getByRole('checkbox', {name: /internal only/i})).toBeChecked();
    });

    it('reopens a saved component-referenced input in edit mode with its component selectors visible', () => {
        render(
            <EditHarness
                defaultValues={
                    {
                        componentReference: {componentName: 'slack', componentVersion: 2, groupName: 'channel'},
                        label: 'Channel',
                        name: 'channelId',
                        required: false,
                        type: 'component',
                    } as WorkflowInputType
                }
            />
        );

        expect(screen.getByText('Edit Input')).toBeInTheDocument();
        expect(screen.getByText('Configured at deployment time.')).toBeInTheDocument();
        expect(screen.getByText('Test Value')).toBeInTheDocument();

        const nameInput = screen.getByPlaceholderText('Input name (will be used as a dynamic value key)');

        expect(nameInput).toHaveValue('channelId');
        expect(nameInput).toHaveAttribute('readonly');
    });
    it('should keep the existing test value when an input is opened for editing', async () => {
        render(
            <EditHarness
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

    it('should keep the existing test value when the form is reset with the opened input', async () => {
        const saveWorkflowInput = vi.fn();

        render(
            <ResetOnOpenHarness
                openedInput={{
                    label: 'Count',
                    name: 'count',
                    required: false,
                    testValue: '42',
                    type: 'integer',
                }}
                saveWorkflowInput={saveWorkflowInput}
            />
        );

        await waitFor(() => expect(screen.getByLabelText('Test Value')).toHaveValue(42));

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInput).toHaveBeenCalled());

        expect(saveWorkflowInput.mock.calls[0][0]).toMatchObject({name: 'count', testValue: '42'});
    });

    it('should clear the test value when the input type changes', async () => {
        render(
            <EditHarness
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
            render(<Harness defaultValues={newInputDefaultValues(name)} />);

            fireEvent.click(await screen.findByRole('button', {name: 'Save'}));

            expect(
                await screen.findByText(
                    'Name must start with a letter or underscore and contain only letters, digits and underscores'
                )
            ).toBeInTheDocument();

            expect(saveWorkflowInputMock).not.toHaveBeenCalled();
        }
    );

    it.each(['myInput', 'my_input', '_leading', 'input1', 'INPUT_2'])('should accept the name %s', async (name) => {
        render(<Harness defaultValues={newInputDefaultValues(name)} />);

        fireEvent.click(await screen.findByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInputMock).toHaveBeenCalled());
    });

    it('leaves the declaration editable for a visually built workflow', () => {
        render(
            <EditHarness
                defaultValues={
                    {label: 'Order ID', name: 'orderId', required: false, type: 'string'} as WorkflowInputType
                }
            />
        );

        expect(screen.getByPlaceholderText('Input label')).not.toHaveAttribute('readonly');
        expect(screen.getByRole('checkbox', {name: 'Required'})).not.toBeDisabled();
    });
});

describe('WorkflowInputsEditDialog input definition', () => {
    beforeEach(() => {
        windowResizeObserver();

        mockScrollIntoView();
    });

    it('should describe editing when an existing input is opened', () => {
        render(<EditDialogHarness defaultValues={newInputDefaultValues('ticket')} />);

        expect(screen.getByText('Edit Input')).toBeInTheDocument();
        expect(screen.getByText('Edit the workflow input definition.')).toBeInTheDocument();
        expect(screen.queryByText('Add a new workflow input definition.')).not.toBeInTheDocument();
    });

    it('should describe adding when a new input is created', () => {
        render(<EditDialogHarness currentInputIndex={-1} defaultValues={newInputDefaultValues('ticket')} />);

        expect(screen.getByText('Create a new Input')).toBeInTheDocument();
        expect(screen.getByText('Add a new workflow input definition.')).toBeInTheDocument();
    });
});
