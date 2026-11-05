import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorCanvas from '../useWorkflowEditorCanvas';

const hoisted = vi.hoisted(() => ({
    useInitialViewportMock: vi.fn(),
    useNodesInitializedMock: vi.fn(),
    useReactFlowMock: vi.fn(),
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useNodesInitialized: hoisted.useNodesInitializedMock,
    useReactFlow: hoisted.useReactFlowMock,
}));

vi.mock('../useInitialViewport', () => ({default: hoisted.useInitialViewportMock}));

vi.mock('../useOverlayPanelsViewport', () => ({default: () => ({getViewportOffsetX: () => 0})}));

vi.mock('../useLayout', () => ({default: () => undefined}));

vi.mock('../useHandleDrop', () => ({default: () => [vi.fn(), vi.fn(), vi.fn(), vi.fn()]}));

vi.mock('../useStickyNotes', () => ({default: () => ({handleAddStickyNote: vi.fn()})}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: {isPending: false, mutate: vi.fn()},
    }),
}));

const renderCanvasHook = (fitViewOnWorkflowChange?: boolean) =>
    renderHook(() =>
        useWorkflowEditorCanvas({
            componentDefinitions: [],
            fitViewOnWorkflowChange,
            taskDispatcherDefinitions: [],
        })
    );

describe('useWorkflowEditorCanvas fit view on workflow change', () => {
    beforeEach(() => {
        hoisted.useInitialViewportMock.mockClear();
        hoisted.useNodesInitializedMock.mockClear();
        hoisted.useReactFlowMock.mockClear();

        useWorkflowDataStore.setState({
            workflow: {id: 'workflow-1', nodeNames: [], version: 1, workflowUuid: 'workflow-uuid-1'},
        });
    });

    it('disables the initial viewport while fitting the view on workflow change', () => {
        renderCanvasHook(true);

        expect(hoisted.useInitialViewportMock).toHaveBeenLastCalledWith(expect.objectContaining({enabled: false}));
    });

    it('keeps the initial viewport enabled by default', () => {
        renderCanvasHook();

        expect(hoisted.useInitialViewportMock).toHaveBeenLastCalledWith(expect.objectContaining({enabled: true}));
    });

    it('never subscribes to React Flow state, so editors without the fit view do not re-render on it', () => {
        renderCanvasHook(true);
        renderCanvasHook(false);

        expect(hoisted.useNodesInitializedMock).not.toHaveBeenCalled();
        expect(hoisted.useReactFlowMock).not.toHaveBeenCalled();
    });
});
