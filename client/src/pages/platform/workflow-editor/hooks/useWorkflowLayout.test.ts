import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowLayout} from './useWorkflowLayout';

const hoisted = vi.hoisted(() => ({
    routeParams: {} as Record<string, string | undefined>,
}));

vi.mock('react-router-dom', () => ({
    useParams: () => hoisted.routeParams,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        useGetComponentDefinitionsQuery: () => ({data: undefined, error: null, isLoading: false}),
    }),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: undefined, error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    useGetPreviousWorkflowNodeOutputsQuery: () => ({data: undefined, isPending: false}),
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationQuery: () => ({data: undefined}),
}));

beforeEach(() => {
    hoisted.routeParams = {};

    useCopilotPanelStore.setState({copilotPanelOpen: false});
    useCopilotStore.setState({
        context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
    });
    useWorkflowDataStore.setState({workflow: {id: 'workflow-1', nodeNames: []}});
});

describe('useWorkflowLayout', () => {
    it('opens the project workflow editor Copilot when no integration is in the route', () => {
        hoisted.routeParams = {projectId: '1', projectWorkflowId: '2'};

        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });

    it('opens the embedded workflow editor Copilot when an integration is in the route', () => {
        hoisted.routeParams = {integrationId: '7'};

        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {integrationId: '7'},
            source: Source.WORKFLOW_EDITOR_EMBEDDED,
        });
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });
});
