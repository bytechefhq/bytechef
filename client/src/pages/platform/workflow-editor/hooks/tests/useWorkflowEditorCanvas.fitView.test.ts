import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorCanvas from '../useWorkflowEditorCanvas';

const hoisted = vi.hoisted(() => ({
    fitViewMock: vi.fn(),
    nodesInitialized: {current: false},
    useInitialViewportMock: vi.fn(),
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useNodesInitialized: () => hoisted.nodesInitialized.current,
    useReactFlow: () => ({fitView: hoisted.fitViewMock}),
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
        hoisted.fitViewMock.mockClear();
        hoisted.useInitialViewportMock.mockClear();
        hoisted.nodesInitialized.current = false;

        useWorkflowDataStore.setState({
            workflow: {id: 'workflow-1', nodeNames: [], version: 1, workflowUuid: 'workflow-uuid-1'},
        });
    });

    it('fits the view once after the nodes are initialised', () => {
        const {rerender} = renderCanvasHook(true);

        expect(hoisted.fitViewMock).not.toHaveBeenCalled();

        hoisted.nodesInitialized.current = true;

        rerender();
        rerender();

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(1);
    });

    it('fits the view again when the workflow changes', () => {
        hoisted.nodesInitialized.current = true;

        const {rerender} = renderCanvasHook(true);

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(1);

        useWorkflowDataStore.setState({
            workflow: {id: 'workflow-2', nodeNames: [], version: 1, workflowUuid: 'workflow-uuid-2'},
        });

        rerender();

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(2);
    });

    it('disables the initial viewport while fitting the view on workflow change', () => {
        renderCanvasHook(true);

        expect(hoisted.useInitialViewportMock).toHaveBeenLastCalledWith(expect.objectContaining({enabled: false}));
    });

    it('never fits the view when fitViewOnWorkflowChange is off', () => {
        hoisted.nodesInitialized.current = true;

        const {rerender} = renderCanvasHook(false);

        rerender();

        expect(hoisted.fitViewMock).not.toHaveBeenCalled();
        expect(hoisted.useInitialViewportMock).toHaveBeenLastCalledWith(expect.objectContaining({enabled: true}));
    });

    it('keeps the initial viewport enabled by default', () => {
        hoisted.nodesInitialized.current = true;

        renderCanvasHook();

        expect(hoisted.fitViewMock).not.toHaveBeenCalled();
        expect(hoisted.useInitialViewportMock).toHaveBeenLastCalledWith(expect.objectContaining({enabled: true}));
    });
});
