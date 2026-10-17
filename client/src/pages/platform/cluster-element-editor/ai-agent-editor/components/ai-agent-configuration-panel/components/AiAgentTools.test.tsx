import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, within} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import AiAgentTools from './AiAgentTools';

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('./AiAgentTool', () => ({
    default: ({tool}: {tool: {name: string}}) => <span>{`Tool ${tool.name}`}</span>,
}));

vi.mock('./AiAgentToolDropdownMenu', () => ({
    default: ({tool}: {tool: {name: string}}) => <button type="button">{`Tool menu of ${tool.name}`}</button>,
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

    it("offers the gate's configure and remove menu in its group heading", () => {
        render(
            <TooltipProvider>
                <AiAgentTools />
            </TooltipProvider>
        );

        const toolGroup = screen.getByRole('group', {name: 'Approval Gate requires approval'});

        expect(within(toolGroup).getByRole('button', {name: 'Tool menu of approvalGate_1'})).toBeInTheDocument();
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
