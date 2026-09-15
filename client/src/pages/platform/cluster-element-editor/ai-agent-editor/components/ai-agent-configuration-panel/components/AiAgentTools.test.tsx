import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, screen, within} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useAiAgentToolsMock} = vi.hoisted(() => ({
    useAiAgentToolsMock: vi.fn(),
}));

vi.mock('./hooks/useAiAgentTools', () => ({
    default: useAiAgentToolsMock,
}));

vi.mock('./AiAgentTool', () => ({
    default: ({tool}: {tool: {label: string; name: string}}) => <div>{`Tool ${tool.name} ${tool.label}`}</div>,
}));

vi.mock('./AiAgentToolDropdownMenu', () => ({
    default: ({tool}: {tool: {name: string}}) => <button type="button">{`Tool menu of ${tool.name}`}</button>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div data-testid="add-tool-popover">{children}</div>,
}));

import AiAgentTools from './AiAgentTools';

const APPROVAL_GATE_TOOL_GROUP = {
    channels: [
        {
            clusterElementType: 'approvalChannels',
            componentName: 'slack',
            componentVersion: 1,
            label: 'slack_1',
            name: 'slack_1',
            operationName: 'approvalChannel',
            title: 'Slack',
            type: 'slack/v1/approvalChannel',
        },
    ],
    label: 'Approval Gate',
    name: 'approvalGate_1',
    tool: {
        clusterElementType: 'tools',
        componentName: 'aiAgentUtils',
        componentVersion: 1,
        label: 'Approval Gate',
        name: 'approvalGate_1',
        operationName: 'approvalGate',
        title: 'AI Agent Utils',
        type: 'aiAgentUtils/v1/approvalGate',
    },
    tools: [],
};

const renderTools = (readOnly = false) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <TooltipProvider>
                <AiAgentTools />
            </TooltipProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

const mockTools = (
    tools: Array<{label: string; name: string}>,
    toolGroups: Array<typeof APPROVAL_GATE_TOOL_GROUP> = []
) =>
    useAiAgentToolsMock.mockReturnValue({
        configuredConnectionKeys: new Set(),
        rootWorkflowNodeName: 'aiAgent_1',
        toolGroups,
        tools,
    });

describe('AiAgentTools', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('names each gated tool group for assistive technology', () => {
        mockTools([], [APPROVAL_GATE_TOOL_GROUP]);

        renderTools();

        expect(screen.getByRole('group', {name: 'Approval Gate requires approval'})).toBeInTheDocument();
    });

    it("offers the gate's configure and remove menu in its group heading", () => {
        mockTools([], [APPROVAL_GATE_TOOL_GROUP]);

        renderTools();

        const toolGroup = screen.getByRole('group', {name: 'Approval Gate requires approval'});

        expect(within(toolGroup).getByRole('button', {name: 'Tool menu of approvalGate_1'})).toBeInTheDocument();
    });

    it('lists each approval channel as an item with its configure and remove menu', () => {
        mockTools([], [APPROVAL_GATE_TOOL_GROUP]);

        renderTools();

        const toolGroup = screen.getByRole('group', {name: 'Approval Gate requires approval'});

        expect(within(toolGroup).getByText('Tool slack_1 slack_1')).toBeInTheDocument();
    });

    it('offers Add Tool when the editor is editable', () => {
        mockTools([]);

        renderTools(false);

        expect(screen.getByRole('button', {name: 'Add Tool'})).toBeInTheDocument();
        expect(screen.getByTestId('add-tool-popover')).toBeInTheDocument();
        expect(screen.getByText(/Click "Add tool"/)).toBeInTheDocument();
    });

    it('offers no Add Tool affordance in read-only mode', () => {
        mockTools([]);

        renderTools(true);

        expect(screen.queryByRole('button', {name: 'Add Tool'})).not.toBeInTheDocument();
        expect(screen.queryByTestId('add-tool-popover')).not.toBeInTheDocument();
        expect(screen.getByText('No tools added yet.')).toBeInTheDocument();
    });

    it('still lists the configured tools in read-only mode', () => {
        mockTools([{label: 'Get a random quote', name: 'httpClient_1'}]);

        renderTools(true);

        expect(screen.getByText('Tool httpClient_1 Get a random quote')).toBeInTheDocument();
    });
});
