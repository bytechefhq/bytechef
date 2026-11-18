import WorkflowInputsEditDialog from '@/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsEditDialog';
import {WorkflowInputType} from '@/shared/types';
import {mockScrollIntoView, windowResizeObserver} from '@/shared/util/test-utils';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useEffect, useRef} from 'react';
import {useForm} from 'react-hook-form';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

const {useGetComponentDefinitionQueryMock, workflowEditorState} = vi.hoisted(() => ({
    useGetComponentDefinitionQueryMock: vi.fn(),
    workflowEditorState: {
        codeWorkflow: false,
        componentDefinitions: [] as Array<{inputsCount: number; name: string; version: number}>,
    },
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: (request: unknown, enabled: boolean) =>
        useGetComponentDefinitionQueryMock(request, enabled),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        codeWorkflow: workflowEditorState.codeWorkflow,
        useGetComponentDefinitionsQuery: () => ({data: workflowEditorState.componentDefinitions}),
    }),
}));

const workflow = {
    definition: JSON.stringify({tasks: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}], triggers: []}),
    label: 'Test',
    workflowTaskComponentNames: ['slack'],
} as never;

const savedChannelInput = {
    componentReference: {componentName: 'slack', componentVersion: 2, groupName: 'channel'},
    label: 'Channel',
    name: 'channel',
    required: false,
    testValue: 'C123',
    type: 'component',
} as WorkflowInputType;

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

interface HarnessProps {
    defaultValues?: WorkflowInputType;
    harnessWorkflow?: never;
}

const Harness = ({
    defaultValues = {label: '', name: '', required: false},
    harnessWorkflow = workflow,
}: HarnessProps) => {
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
            workflow={harnessWorkflow}
        />
    );
};

interface EditHarnessProps {
    defaultValues: WorkflowInputType;
    harnessWorkflow?: never;
}

const EditHarness = ({defaultValues, harnessWorkflow = workflow}: EditHarnessProps) => {
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
            workflow={harnessWorkflow}
        />
    );
};

const selectOption = async (user: ReturnType<typeof userEvent.setup>, triggerText: string, optionName: string) => {
    await user.click(await screen.findByText(triggerText));

    const listbox = await screen.findByRole('listbox');

    await user.click(within(listbox).getByRole('option', {name: optionName}));
};

const TYPE_COMBOBOX_INDEX = 0;
const COMPONENT_COMBOBOX_INDEX = 1;
const GROUP_COMBOBOX_INDEX = 2;

const selectComboboxOption = async (
    user: ReturnType<typeof userEvent.setup>,
    comboboxIndex: number,
    optionName: string
) => {
    const comboboxes = await screen.findAllByRole('combobox');

    await user.click(comboboxes[comboboxIndex]);

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
        workflowEditorState.codeWorkflow = false;
        workflowEditorState.componentDefinitions = [
            {inputsCount: 1, name: 'googleSheets', version: 1},
            {inputsCount: 1, name: 'slack', version: 2},
        ];

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

    it('explains why the Component select is empty when no workflow component offers inputs', async () => {
        workflowEditorState.componentDefinitions = [{inputsCount: 0, name: 'slack', version: 2}];

        const user = setupUser();

        render(<Harness />);

        await selectOption(user, 'Select input type', 'Component property');
        await user.click(await screen.findByText('Select component'));

        const listbox = await screen.findByRole('listbox');

        expect(within(listbox).queryByRole('option')).not.toBeInTheDocument();
        expect(within(listbox).getByText(/No component in this workflow has inputs to offer/)).toBeInTheDocument();
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
    it('should block saving a component property input until an input is selected', async () => {
        const user = setupUser();

        render(<Harness defaultValues={{label: 'Channel', name: 'channel', required: false}} />);

        await selectOption(user, 'Select input type', 'Component property');
        await selectOption(user, 'Select component', 'slack');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await selectOption(user, 'Select input', 'Channel');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInputMock).toHaveBeenCalled());

        expect(saveWorkflowInputMock).toHaveBeenCalledOnce();
        expect(saveWorkflowInputMock.mock.calls[0][0].componentReference.groupName).toBe('channel');
    });

    it('should drop the component reference when a component input is switched to another type', async () => {
        const user = setupUser();

        render(<EditHarness defaultValues={savedChannelInput} />);

        await selectComboboxOption(user, TYPE_COMBOBOX_INDEX, 'String');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInputMock).toHaveBeenCalled());

        const savedInput = saveWorkflowInputMock.mock.calls[0][0];

        expect(savedInput.type).toBe('string');
        expect(savedInput.componentReference).toBeUndefined();
    });

    it('should show empty component selectors after switching a component input away and back', async () => {
        const user = setupUser();

        render(<EditHarness defaultValues={savedChannelInput} />);

        await selectComboboxOption(user, TYPE_COMBOBOX_INDEX, 'String');
        await selectComboboxOption(user, TYPE_COMBOBOX_INDEX, 'Component property');

        expect(await screen.findByText('Select component')).toBeInTheDocument();
        expect(screen.getByText('Select input')).toBeInTheDocument();
    });

    it('should resolve the version of a component used only inside a task dispatcher', async () => {
        const user = setupUser();

        const nestedWorkflow = {
            definition: JSON.stringify({
                tasks: [
                    {
                        name: 'loop_1',
                        parameters: {iteratee: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}]},
                        type: 'loop/v1',
                    },
                ],
                triggers: [],
            }),
            label: 'Test',
            workflowTaskComponentNames: ['loop', 'slack'],
        } as never;

        render(<Harness harnessWorkflow={nestedWorkflow} />);

        await selectOption(user, 'Select input type', 'Component property');
        await selectOption(user, 'Select component', 'slack');
        await selectOption(user, 'Select input', 'Channel');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInputMock).toHaveBeenCalled());

        expect(saveWorkflowInputMock.mock.calls[0][0].componentReference.componentVersion).toBe(2);
        expect(useGetComponentDefinitionQueryMock).toHaveBeenLastCalledWith(
            {componentName: 'slack', componentVersion: 2},
            true
        );
    });

    it('should keep the saved component version when the workflow uses the component in two versions', () => {
        const twoVersionsWorkflow = {
            definition: JSON.stringify({
                tasks: [
                    {name: 'sendMessage_1', type: 'slack/v1/sendMessage'},
                    {name: 'sendMessage_2', type: 'slack/v2/sendMessage'},
                ],
                triggers: [],
            }),
            label: 'Test',
            workflowTaskComponentNames: ['slack'],
        } as never;

        render(
            <EditHarness
                defaultValues={{
                    ...savedChannelInput,
                    componentReference: {componentName: 'slack', componentVersion: 1, groupName: 'channel'},
                }}
                harnessWorkflow={twoVersionsWorkflow}
            />
        );

        expect(useGetComponentDefinitionQueryMock).toHaveBeenLastCalledWith(
            {componentName: 'slack', componentVersion: 1},
            true
        );
    });

    it('should reset the test value when another input of the component is selected', async () => {
        const user = setupUser();

        render(<EditHarness defaultValues={savedChannelInput} />);

        await selectComboboxOption(user, GROUP_COMBOBOX_INDEX, 'Date Range');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInputMock).toHaveBeenCalled());

        const savedInput = saveWorkflowInputMock.mock.calls[0][0];

        expect(savedInput.componentReference.groupName).toBe('dateRange');
        expect(savedInput.testValue).not.toBe('C123');
    });

    it('should reset the test value when another component is selected', async () => {
        const user = setupUser();

        const twoComponentsWorkflow = {
            definition: JSON.stringify({
                tasks: [
                    {name: 'sendMessage_1', type: 'slack/v2/sendMessage'},
                    {name: 'getRows_1', type: 'googleSheets/v1/getRows'},
                ],
                triggers: [],
            }),
            label: 'Test',
            workflowTaskComponentNames: ['googleSheets', 'slack'],
        } as never;

        render(<EditHarness defaultValues={savedChannelInput} harnessWorkflow={twoComponentsWorkflow} />);

        await selectComboboxOption(user, COMPONENT_COMBOBOX_INDEX, 'googleSheets');
        await selectOption(user, 'Select input', 'Channel');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(saveWorkflowInputMock).toHaveBeenCalled());

        const savedInput = saveWorkflowInputMock.mock.calls[0][0];

        expect(savedInput.componentReference).toEqual({
            componentName: 'googleSheets',
            componentVersion: 1,
            groupName: 'channel',
        });
        expect(savedInput.testValue).not.toBe('C123');
    });

    it('should lock the declaration but not the test value for a code workflow', async () => {
        workflowEditorState.codeWorkflow = true;

        render(
            <EditHarness
                defaultValues={{
                    label: 'Channel',
                    name: 'channel',
                    required: false,
                    testValue: '#alerts',
                    type: 'string',
                }}
            />
        );

        expect(await screen.findByPlaceholderText('Input label')).toHaveAttribute('readonly');
        expect(screen.getByLabelText('Test Value')).not.toHaveAttribute('readonly');

        for (const combobox of screen.getAllByRole('combobox')) {
            expect(combobox).toBeDisabled();
        }
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
