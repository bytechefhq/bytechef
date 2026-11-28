import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useCopilotWorkflowEditorBinding from '@/shared/components/copilot/hooks/useCopilotWorkflowEditorBinding';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useWorkflowLayout} from './useWorkflowLayout';

const hoisted = vi.hoisted(() => ({
    matchedPath: undefined as string | undefined,
    routeParams: {} as Record<string, string | undefined>,
}));

vi.mock('react-router-dom', () => ({
    useMatch: (pattern: string) => (pattern === hoisted.matchedPath ? {params: hoisted.routeParams} : null),
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
    hoisted.matchedPath = undefined;
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
            mode: MODE.BUILD,
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
            mode: MODE.BUILD,
            parameters: {integrationId: '7'},
            source: Source.WORKFLOW_EDITOR_EMBEDDED,
        });
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });

    it('opens the embedded automation workflow editor Copilot on the embedded automation editor route', () => {
        hoisted.matchedPath = '/embedded/automations/:workflowUuid/editor';
        hoisted.routeParams = {workflowUuid: 'workflow-uuid'};

        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.BUILD,
            parameters: {},
            source: Source.WORKFLOW_EDITOR_EMBEDDED_AUTOMATION,
        });
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });

    it('opens the project workflow editor Copilot on another route with a workflow uuid', () => {
        hoisted.routeParams = {workflowUuid: 'workflow-uuid'};

        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(useCopilotStore.getState().context.source).toBe(Source.WORKFLOW_EDITOR);
    });

    it('opens the editor Copilot in Ask after a Skills conversation was left in Build', () => {
        hoisted.routeParams = {projectId: '1', projectWorkflowId: '2'};

        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.SKILLS},
        });

        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });
    });

    it('reopens the Copilot in Ask with an empty transcript after switching workflows while the panel was closed', () => {
        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            messages: [{content: 'build workflow A', role: 'user'}],
            workflowEditorWorkflowKey: undefined,
        });

        const {rerender} = renderHook(({workflowKey}) => useCopilotWorkflowEditorBinding(workflowKey), {
            initialProps: {workflowKey: 'project:1:2'},
        });

        hoisted.routeParams = {projectId: '1', projectWorkflowId: '3'};

        rerender({workflowKey: 'project:1:3'});

        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(useCopilotStore.getState().context.mode).toBe(MODE.ASK);
        expect(useCopilotStore.getState().messages).toEqual([]);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });
});
