import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen, within} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import AiAgentTools from './AiAgentTools';

const hoisted = vi.hoisted(() => ({
    handleConfigureTool: vi.fn(),
    handleRemoveTool: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('./AiAgentTool', () => ({
    default: ({tool}: {tool: {name: string}}) => <span>{`Tool ${tool.name}`}</span>,
}));

vi.mock('./hooks/useAiAgentToolActions', () => ({
    default: () => hoisted,
}));

vi.mock('./hooks/useAiAgentTools', () => ({
    default: () => ({
        configuredConnectionKeys: [],
        rootWorkflowNodeName: 'aiAgent_1',
        toolGroups: [
            {
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
            },
        ],
        tools: [],
    }),
}));

describe('AiAgentTools', () => {
    it('names each gated tool group for assistive technology', () => {
        render(
            <TooltipProvider>
                <AiAgentTools />
            </TooltipProvider>
        );

        expect(screen.getByRole('group', {name: 'Approval Gate requires approval'})).toBeInTheDocument();
    });

    it('configures and removes the gate from its group heading', () => {
        render(
            <TooltipProvider>
                <AiAgentTools />
            </TooltipProvider>
        );

        const toolGroup = screen.getByRole('group', {name: 'Approval Gate requires approval'});

        fireEvent.click(within(toolGroup).getByRole('button', {name: 'Configure approval gate'}));
        fireEvent.click(within(toolGroup).getByRole('button', {name: 'Remove approval gate'}));

        expect(hoisted.handleConfigureTool).toHaveBeenCalledWith(expect.objectContaining({name: 'approvalGate_1'}));
        expect(hoisted.handleRemoveTool).toHaveBeenCalledWith(expect.objectContaining({name: 'approvalGate_1'}));
    });

    it('lists each approval channel as an item with its configure and remove menu', () => {
        render(
            <TooltipProvider>
                <AiAgentTools />
            </TooltipProvider>
        );

        const toolGroup = screen.getByRole('group', {name: 'Approval Gate requires approval'});

        expect(within(toolGroup).getByText('Tool slack_1')).toBeInTheDocument();
    });
});
