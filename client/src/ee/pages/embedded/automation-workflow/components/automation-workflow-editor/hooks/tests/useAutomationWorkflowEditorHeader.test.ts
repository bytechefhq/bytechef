import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useAutomationWorkflowEditorHeader} from '../useAutomationWorkflowEditorHeader';

const hoisted = vi.hoisted(() => ({
    publishMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationWorkflowProjectsQuery: () => ({
        data: {automationWorkflowProjects: [{id: 'project-1', name: 'Mailing', workflowTemplates: []}]},
    }),
    usePublishAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutate}),
}));

vi.mock('@/shared/hooks/useWorkflowTestStream', () => ({
    useWorkflowTestStream: () => ({
        close: vi.fn(),
        error: null,
        getPersistedJobId: vi.fn(),
        persistJobId: vi.fn(),
        setStreamRequest: vi.fn(),
    }),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectWorkflowTested: vi.fn()}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

describe('useAutomationWorkflowEditorHeader', () => {
    beforeEach(() => {
        hoisted.publishMutate.mockReset();
    });

    it('publishes the project with the description entered in the publish popover', () => {
        const onSuccess = vi.fn();

        const {result} = renderHook(
            () => useAutomationWorkflowEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        result.current.handlePublishProjectSubmit({description: 'Adds greeting', onSuccess});

        expect(hoisted.publishMutate).toHaveBeenCalledWith(
            {description: 'Adds greeting', id: 'project-1'},
            {onSuccess}
        );
    });
});
