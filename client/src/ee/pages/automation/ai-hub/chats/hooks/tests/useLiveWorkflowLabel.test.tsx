import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubChatI} from '../../api/chats.api';

const {hasWorkspaceScopeMock, queryOptionsRef} = vi.hoisted(() => ({
    hasWorkspaceScopeMock: vi.fn(),
    queryOptionsRef: {current: undefined as {enabled?: boolean} | undefined},
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: hasWorkspaceScopeMock,
}));

vi.mock('@/shared/hooks/useLoadWorkspaceScopes', () => ({
    useLoadWorkspaceScopes: () => ({error: false, loading: false}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 3}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useWorkspaceChatWorkflowsQuery: (_variables: unknown, options: {enabled?: boolean}) => {
        queryOptionsRef.current = options;

        return {
            data: options.enabled
                ? {
                      workspaceChatWorkflows: [
                          {
                              projectDeploymentId: '5',
                              projectId: '1',
                              projectName: 'Acme',
                              projectWorkflowId: '8',
                              workflowExecutionId: 'exec-1',
                              workflowId: '9',
                              workflowLabel: 'Support',
                          },
                      ],
                  }
                : undefined,
        };
    },
}));

const {useLiveWorkflowLabel} = await import('../useLiveWorkflowLabel');

function buildChat(overrides: Partial<AiHubChatI> = {}): AiHubChatI {
    return {
        autoTitled: false,
        createdAt: '',
        environmentId: 2,
        id: 1,
        isOwner: true,
        kind: 'WORKFLOW_CHAT',
        lastPreview: null,
        messageCount: 0,
        ownerUserId: 1,
        projectDeploymentId: '5',
        status: 'ACTIVE',
        threadId: 'thread-1',
        title: null,
        updatedAt: '',
        userId: 1,
        workflowExecutionId: 'exec-1',
        workspaceId: 3,
        ...overrides,
    };
}

describe('useLiveWorkflowLabel', () => {
    beforeEach(() => {
        hasWorkspaceScopeMock.mockReset();
        hasWorkspaceScopeMock.mockReturnValue(true);
    });

    it('returns the live "project - workflow" label for a listed workflow chat', () => {
        const {result} = renderHook(() => useLiveWorkflowLabel(buildChat()));

        expect(result.current).toBe('Acme — Support');
    });

    it('returns null for a workflow chat whose workflow is no longer listed', () => {
        const {result} = renderHook(() => useLiveWorkflowLabel(buildChat({workflowExecutionId: 'gone'})));

        expect(result.current).toBeNull();
    });

    it('returns null and disables the query for a standard chat', () => {
        const {result} = renderHook(() =>
            useLiveWorkflowLabel(buildChat({kind: 'STANDARD', projectDeploymentId: null, workflowExecutionId: null}))
        );

        expect(result.current).toBeNull();
        expect(queryOptionsRef.current?.enabled).toBe(false);
    });

    it('returns null and never runs the query for a user without WORKFLOW_VIEW', () => {
        hasWorkspaceScopeMock.mockReturnValue(false);

        const {result} = renderHook(() => useLiveWorkflowLabel(buildChat()));

        expect(hasWorkspaceScopeMock).toHaveBeenCalledWith(3, 'WORKFLOW_VIEW');
        expect(queryOptionsRef.current?.enabled).toBe(false);
        expect(result.current).toBeNull();
    });
});
