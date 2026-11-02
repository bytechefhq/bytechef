import {renderHook, waitFor} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowEditorReadOnlyProvider from '../../providers/WorkflowEditorReadOnlyProvider';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {STICKY_NOTE_NODE_TYPE} from '../../utils/stickyNoteUtils';
import useLayout from '../useLayout';

vi.mock('../../utils/layoutUtils', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../utils/layoutUtils')>();

    return {
        ...actual,
        getLayoutElements: vi.fn(({edges, nodes}: {edges: unknown[]; nodes: Node[]}) =>
            Promise.resolve({edges, nodes})
        ),
    };
});

const STICKY_NOTE_DEFINITION = JSON.stringify({
    metadata: {
        ui: {
            stickyNotes: [{color: 'yellow', content: 'note', id: 'stickyNote_1', position: {x: 0, y: 0}}],
        },
    },
    tasks: [],
    triggers: [],
});

const renderLayout = (readOnly: boolean) =>
    renderHook(() => useLayout({canvasWidth: 800, componentDefinitions: [], taskDispatcherDefinitions: []}), {
        wrapper: ({children}: {children: ReactNode}) => (
            <WorkflowEditorReadOnlyProvider readOnly={readOnly}>{children}</WorkflowEditorReadOnlyProvider>
        ),
    });

const findStickyNoteNode = () =>
    useWorkflowDataStore.getState().nodes.find((node) => node.type === STICKY_NOTE_NODE_TYPE);

describe('useLayout sticky notes', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState((state) => ({
            edges: [],
            isNodeDragging: false,
            isWorkflowLoaded: true,
            nodes: [],
            savedPositionCrossAxisShift: 0,
            workflow: {
                ...state.workflow,
                definition: STICKY_NOTE_DEFINITION,
                id: 'workflow-1',
                tasks: [],
                triggers: [],
                version: 1,
            },
        }));
    });

    it('builds draggable, editable sticky notes in an editable editor', async () => {
        renderLayout(false);

        await waitFor(() => expect(findStickyNoteNode()).toBeDefined());

        const stickyNoteNode = findStickyNoteNode()!;

        expect(stickyNoteNode.draggable).toBe(true);
        expect((stickyNoteNode.data as {readOnly?: boolean}).readOnly).toBe(false);
    });

    it('builds locked sticky notes under the read-only provider', async () => {
        renderLayout(true);

        await waitFor(() => expect(findStickyNoteNode()).toBeDefined());

        const stickyNoteNode = findStickyNoteNode()!;

        expect(stickyNoteNode.draggable).toBe(false);
        expect(stickyNoteNode.selectable).toBe(false);
        expect((stickyNoteNode.data as {readOnly?: boolean}).readOnly).toBe(true);
    });
});
