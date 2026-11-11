import {McpToolFormChangeHandlerType} from '@/pages/platform/mcp-servers/components/McpComponentDialogToolForm';
import {fireEvent, render, resetAll, screen, waitFor} from '@/shared/util/test-utils';
import {UseFormReturn} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentDialogToolSelectionStep from '../McpComponentDialogToolSelectionStep';
import {SelectedToolI} from '../hooks/useMcpComponentDialogToolSelectionStep';

const hoisted = vi.hoisted(() => ({
    clusterElementDefinitionResult: {
        data: {
            clusterElementDefinition: {
                properties: [{controlType: 'TEXT', name: 'message', type: 'STRING'}],
            },
        },
        isLoading: false,
    },
    isLoadingComponentDefinition: false,
    toolElements: [] as Array<{description?: string; name: string; title?: string}>,
}));

vi.mock('../hooks/useMcpComponentDialogToolSelectionStep', () => ({
    default: () => ({
        allToolsSelected: false,
        componentDefinition: undefined,
        handleSelectAllTools: vi.fn(),
        handleToolToggle: vi.fn(),
        isLoadingComponentDefinition: hoisted.isLoadingComponentDefinition,
        selectAllCheckboxRef: {current: null},
        toolElements: hoisted.toolElements,
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useClusterElementDefinitionQuery: () => hoisted.clusterElementDefinitionResult,
}));

vi.mock('@/pages/platform/mcp-servers/components/McpComponentToolForm', () => ({
    default: ({form, toolName}: {form: UseFormReturn<Record<string, unknown>>; toolName: string}) => (
        <input aria-label={`${toolName} message`} {...form.register('message')} />
    ),
}));

const sendMessageTool: SelectedToolI = {componentName: 'slack', componentVersion: 1, name: 'sendMessage'};

interface StepOptionsI {
    aiValuesByTool?: Record<string, Record<string, string>>;
    onToolFormChange?: McpToolFormChangeHandlerType;
    savedParametersByToolName?: Map<string, Record<string, unknown> | null | undefined>;
    selectedTools?: SelectedToolI[];
}

const buildStep = ({
    aiValuesByTool = {},
    onToolFormChange = vi.fn(),
    savedParametersByToolName = new Map(),
    selectedTools = [],
}: StepOptionsI) => (
    <McpComponentDialogToolSelectionStep
        aiValuesByTool={aiValuesByTool}
        onToolFormChange={onToolFormChange}
        onToolsChange={vi.fn()}
        savedParametersByToolName={savedParametersByToolName}
        selectedComponent={null}
        selectedTools={selectedTools}
    />
);

const renderStep = (options: StepOptionsI = {}) => render(buildStep(options));

describe('McpComponentDialogToolSelectionStep', () => {
    beforeEach(() => {
        hoisted.isLoadingComponentDefinition = false;
        hoisted.toolElements = [];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows skeleton tool rows while the component definition loads', () => {
        hoisted.isLoadingComponentDefinition = true;

        renderStep();

        expect(screen.getByRole('status', {name: 'Loading tools'})).toBeInTheDocument();
        expect(screen.queryByText('No tools available for this component.')).not.toBeInTheDocument();
        expect(screen.queryByTestId('mcp-tools-heading-skeleton')).not.toBeInTheDocument();
    });

    it('lists the tools once the component definition has loaded', () => {
        hoisted.toolElements = [{description: 'Sends a message', name: 'sendMessage', title: 'Send Message'}];

        renderStep();

        expect(screen.queryByRole('status', {name: 'Loading tools'})).not.toBeInTheDocument();
        expect(screen.getByText('Send Message')).toBeInTheDocument();
    });

    describe('inline tool forms', () => {
        beforeEach(() => {
            hoisted.toolElements = [{name: 'sendMessage', title: 'Send Message'}];
        });

        it('expands the tool form when a tool is ticked', () => {
            const onToolFormChange = vi.fn();

            const {rerender} = renderStep({onToolFormChange});

            expect(screen.queryByRole('group', {name: 'Send Message settings'})).not.toBeInTheDocument();

            rerender(buildStep({onToolFormChange, selectedTools: [sendMessageTool]}));

            expect(screen.getByRole('group', {name: 'Send Message settings'})).toBeInTheDocument();
            expect(onToolFormChange).toHaveBeenLastCalledWith(
                'sendMessage',
                expect.objectContaining({form: expect.objectContaining({reset: expect.any(Function)})})
            );
        });

        it('collapses and discards the form when a tool is unticked', async () => {
            const onToolFormChange = vi.fn();

            const {rerender} = renderStep({onToolFormChange, selectedTools: [sendMessageTool]});

            fireEvent.change(screen.getByLabelText('sendMessage message'), {target: {value: 'typed'}});

            rerender(buildStep({onToolFormChange, selectedTools: []}));

            expect(screen.queryByRole('group', {name: 'Send Message settings'})).not.toBeInTheDocument();
            expect(onToolFormChange).toHaveBeenLastCalledWith('sendMessage', null);

            rerender(buildStep({onToolFormChange, selectedTools: [sendMessageTool]}));

            await waitFor(() => {
                expect(screen.getByLabelText('sendMessage message')).toHaveValue('');
            });
        });

        it('prefills existing tools from saved parameters in edit mode', async () => {
            renderStep({
                savedParametersByToolName: new Map([['sendMessage', {message: 'saved text'}]]),
                selectedTools: [sendMessageTool],
            });

            await waitFor(() => {
                expect(screen.getByLabelText('sendMessage message')).toHaveValue('saved text');
            });
        });

        it('seeds new tools with ai values', async () => {
            renderStep({
                aiValuesByTool: {sendMessage: {message: "=fromAi('message', 'STRING')"}},
                selectedTools: [sendMessageTool],
            });

            await waitFor(() => {
                expect(screen.getByLabelText('sendMessage message')).toHaveValue("=fromAi('message', 'STRING')");
            });
        });

        it('does not re-seed existing tools', async () => {
            renderStep({
                aiValuesByTool: {sendMessage: {message: "=fromAi('message', 'STRING')"}},
                savedParametersByToolName: new Map([['sendMessage', {message: null}]]),
                selectedTools: [sendMessageTool],
            });

            await waitFor(() => {
                expect(screen.getByLabelText('sendMessage message')).toHaveValue('');
            });
        });
    });
});
