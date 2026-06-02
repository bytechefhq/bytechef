import {WorkflowInputType} from '@/shared/types';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useRef} from 'react';
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
            // googleSheets declares inputs but is NOT used in the workflow, so it must be excluded.
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

const Harness = () => {
    const form = useForm<WorkflowInputType>({defaultValues: {label: '', name: '', required: false}});
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
        // Radix Select relies on pointer-capture APIs that jsdom does not implement.
        Element.prototype.hasPointerCapture = vi.fn(() => false);
        Element.prototype.setPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
    });

    beforeEach(() => {
        saveWorkflowInputMock.mockClear();
        useGetComponentDefinitionQueryMock.mockReturnValue({
            data: {
                inputs: [
                    // Lone-property group (the `.inputs(property)` form): no group label, label lives on the property.
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
});
