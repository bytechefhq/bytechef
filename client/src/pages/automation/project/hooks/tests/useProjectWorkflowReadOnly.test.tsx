import {useProjectWorkflowReadOnly} from '@/pages/automation/project/hooks/useProjectWorkflowReadOnly';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    WorkflowEditorReadOnlyContext,
    useWorkflowEditorReadOnly,
} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {render, renderHook, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    grantedScopes: [] as string[],
    requestedEnvironmentIds: [] as Array<number | undefined>,
    requestedWorkspaceIds: [] as Array<number | undefined>,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (workspaceId: number | undefined, scope: string, environmentId?: number) => {
        hoisted.requestedEnvironmentIds.push(environmentId);
        hoisted.requestedWorkspaceIds.push(workspaceId);

        return hoisted.grantedScopes.includes(scope);
    },
}));

const ReadOnlyConsumer = () => <span>{useWorkflowEditorReadOnly() ? 'read-only' : 'editable'}</span>;

const ProjectReadOnlyProvider = () => {
    const readOnly = useProjectWorkflowReadOnly();

    return (
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <ReadOnlyConsumer />
        </WorkflowEditorReadOnlyContext.Provider>
    );
};

describe('useProjectWorkflowReadOnly', () => {
    beforeEach(() => {
        hoisted.grantedScopes = [];
        hoisted.requestedEnvironmentIds = [];
        hoisted.requestedWorkspaceIds = [];

        useWorkspaceStore.setState({currentWorkspaceId: 1049});
    });

    it('is read-only when the member lacks WORKFLOW_EDIT', () => {
        hoisted.grantedScopes = ['WORKFLOW_VIEW', 'WORKFLOW_CREATE', 'WORKFLOW_DELETE'];

        const {result} = renderHook(() => useProjectWorkflowReadOnly());

        expect(result.current).toBe(true);
    });

    it('is editable when the member holds WORKFLOW_EDIT', () => {
        hoisted.grantedScopes = ['WORKFLOW_EDIT'];

        const {result} = renderHook(() => useProjectWorkflowReadOnly());

        expect(result.current).toBe(false);
    });

    it('checks the scope against the current workspace', () => {
        renderHook(() => useProjectWorkflowReadOnly());

        expect(hoisted.requestedWorkspaceIds).toContain(1049);
    });

    it('checks the scope in Development, where the server checks workflow saves', () => {
        renderHook(() => useProjectWorkflowReadOnly());

        expect(hoisted.requestedEnvironmentIds).toEqual([DEVELOPMENT_ENVIRONMENT]);
    });

    it('puts the editor context into read-only mode when the scope is missing', () => {
        render(<ProjectReadOnlyProvider />);

        expect(screen.getByText('read-only')).toBeInTheDocument();
    });

    it('leaves the editor context editable outside a read-only provider', () => {
        render(<ReadOnlyConsumer />);

        expect(screen.getByText('editable')).toBeInTheDocument();
    });
});
