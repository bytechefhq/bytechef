import WorkflowBuilder from '@/ee/pages/embedded/workflow-builder/WorkflowBuilder';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

const {workflowEditorLayoutMock} = vi.hoisted(() => ({workflowEditorLayoutMock: vi.fn()}));

vi.mock('@/ee/pages/embedded/workflow-builder/hooks/useWorkflowBuilder', () => ({
    useWorkflowBuilder: () => ({
        bottomResizablePanelRef: {current: null},
        cancelWorkflowQueries: vi.fn(),
        connectedUserProjectWorkflow: {connectedUserId: 1, workflowVersion: 1},
        includeComponents: undefined,
        initialized: true,
        invalidateWorkflowQueries: vi.fn(),
        projectId: 2,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useRun', () => ({
    useRun: () => ({runDisabled: false}),
}));

vi.mock('@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/WorkflowBuilderHeader', () => ({
    default: () => null,
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    getCreateConnectedUserConnection: () => vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {},
    getConnectedUserConnectionsQuery: () => vi.fn(),
    useGetConnectionTagsQuery: vi.fn(),
}));

vi.mock('@/components/ui/resizable', () => ({
    ResizableHandle: () => null,
    ResizablePanel: ({children}: {children?: ReactNode}) => <div>{children}</div>,
    ResizablePanelGroup: ({children}: {children?: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/WorkflowEditorLayout', () => ({
    default: (props: Record<string, unknown>) => {
        workflowEditorLayoutMock(props);

        return <div data-testid="workflow-editor-layout" />;
    },
}));

describe('WorkflowBuilder', () => {
    it('hides the AI copilot and the workflow inputs panel from the connected user', () => {
        render(<WorkflowBuilder />);

        expect(screen.getByTestId('workflow-editor-layout')).toBeInTheDocument();
        expect(workflowEditorLayoutMock).toHaveBeenCalledWith(
            expect.objectContaining({showCopilot: false, showWorkflowInputs: false})
        );
    });
});
