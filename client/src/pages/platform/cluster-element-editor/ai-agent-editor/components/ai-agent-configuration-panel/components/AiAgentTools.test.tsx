import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import AiAgentTools from './AiAgentTools';

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('./AiAgentTool', () => ({
    default: () => null,
}));

vi.mock('./hooks/useAiAgentTools', () => ({
    default: () => ({
        configuredConnectionKeys: [],
        rootWorkflowNodeName: 'aiAgent_1',
        toolGroups: [{channelLabels: [], label: 'Approval Gate', name: 'approvalGate_1', tools: []}],
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
});
