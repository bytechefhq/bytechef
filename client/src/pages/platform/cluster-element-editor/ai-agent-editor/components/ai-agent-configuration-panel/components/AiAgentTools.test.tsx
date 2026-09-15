import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useAiAgentToolsMock} = vi.hoisted(() => ({
    useAiAgentToolsMock: vi.fn(),
}));

vi.mock('./hooks/useAiAgentTools', () => ({
    default: useAiAgentToolsMock,
}));

vi.mock('./AiAgentTool', () => ({
    default: ({tool}: {tool: {label: string}}) => <div>{tool.label}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div data-testid="add-tool-popover">{children}</div>,
}));

import AiAgentTools from './AiAgentTools';

const renderTools = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <TooltipProvider>
                <AiAgentTools />
            </TooltipProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

const mockTools = (tools: Array<{label: string; name: string}>) =>
    useAiAgentToolsMock.mockReturnValue({
        configuredConnectionKeys: new Set(),
        rootWorkflowNodeName: 'aiAgent_1',
        tools,
    });

describe('AiAgentTools', () => {
    beforeEach(() => {
        vi.clearAllMocks();
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

        expect(screen.getByText('Get a random quote')).toBeInTheDocument();
    });
});
