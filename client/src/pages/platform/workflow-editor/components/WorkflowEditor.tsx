import '@xyflow/react/dist/base.css';
import GraphTransitionEditorLayer from '@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionEditorLayer';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CANVAS_BACKGROUND_COLOR} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {Background, BackgroundVariant, ConnectionMode, ReactFlow} from '@xyflow/react';
import {useEffect} from 'react';
import {useShallow} from 'zustand/react/shallow';

import GraphConnectionLine from '../edges/GraphConnectionLine';
import useWorkflowEditorCanvas from '../hooks/useWorkflowEditorCanvas';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import NodeActionsHint from './NodeActionsHint';
import WorkflowEditorToolbar from './WorkflowEditorToolbar';

type ConditionalWorkflowEditorPropsType =
    | {
          readOnlyWorkflow?: Workflow;
          parentId?: never;
          parentType?: never;
      }
    | {
          readOnlyWorkflow?: never;
      };

type WorkflowEditorPropsType = {
    componentDefinitions: ComponentDefinitionBasic[];
    customCanvasWidth?: number;
    enableUndoRedo?: boolean;
    fitViewOnWorkflowChange?: boolean;
    leftSidebarOpen?: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
};

const WorkflowEditor = ({
    componentDefinitions,
    customCanvasWidth,
    enableUndoRedo,
    fitViewOnWorkflowChange,
    leftSidebarOpen,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: WorkflowEditorPropsType & ConditionalWorkflowEditorPropsType) => {
    const {edges, nodes, onEdgesChange} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
        }))
    );

    const {nodesLocked, setNodesLocked} = useWorkflowEditorStore(
        useShallow((state) => ({
            nodesLocked: state.nodesLocked,
            setNodesLocked: state.setNodesLocked,
        }))
    );

    const {
        edgeTypes,
        handleAddStickyNote,
        handleConnect,
        handleConnectEnd,
        handleNodeDragStart,
        handleNodeDragStop,
        handleNodesChange,
        handleReconnect,
        handleTransitionDeleteKeyDown,
        isValidConnection,
        nodeTypes,
        onDragOver,
        onDrop,
    } = useWorkflowEditorCanvas({
        componentDefinitions,
        customCanvasWidth,
        fitViewOnWorkflowChange,
        leftSidebarOpen,
        readOnlyWorkflow,
        taskDispatcherDefinitions,
    });

    useEffect(() => {
        setNodesLocked(true);
    }, [setNodesLocked]);

    // Registered on the document rather than through React Flow's own `deleteKeyCode`, which stays
    // `null` here: a graph transition is the only thing on this canvas a keypress may delete, and
    // the handler decides that for itself (see `handleTransitionDeleteKeyDown`).
    useEffect(() => {
        document.addEventListener('keydown', handleTransitionDeleteKeyDown);

        return () => document.removeEventListener('keydown', handleTransitionDeleteKeyDown);
    }, [handleTransitionDeleteKeyDown]);

    return (
        <div className="flex h-full flex-1 flex-col rounded-lg bg-background">
            <ReactFlow
                connectionMode={ConnectionMode.Strict}
                deleteKeyCode={null}
                edgeTypes={edgeTypes}
                edges={edges}
                /* Only the graph Start edge opts back in, so no other edge grows drag anchors on
                   its endpoints just because `onReconnect` is wired. */
                edgesReconnectable={false}
                isValidConnection={isValidConnection}
                maxZoom={1.5}
                minZoom={0.001}
                nodeTypes={nodeTypes}
                nodes={nodes}
                /* Graph member transition handles pass their own `isConnectable`, which React Flow
                   consults independently of this flag — so a transition can still be drawn while
                   every other node on the canvas stays unconnectable. */
                nodesConnectable={false}
                nodesDraggable={!readOnlyWorkflow && !nodesLocked}
                onConnect={handleConnect}
                onConnectEnd={handleConnectEnd}
                onDragOver={onDragOver}
                onDrop={onDrop}
                onEdgesChange={onEdgesChange}
                onNodeDragStart={handleNodeDragStart}
                onNodeDragStop={handleNodeDragStop}
                onNodesChange={handleNodesChange}
                onReconnect={handleReconnect}
                panActivationKeyCode={null}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

                {/* The open transition editor, mounted here rather than by the edge it belongs to:
                    React Flow recreates its edge components on every relayout, and an editor inside
                    one lost the caret, the data pill popup and its pending save each time. */}

                <GraphTransitionEditorLayer />

                {/* React Flow suppresses its own connection line while `nodesConnectable` is off,
                    which this canvas needs it to be — see the component for the full reasoning. */}

                <GraphConnectionLine />

                {!readOnlyWorkflow && nodes.length > 0 && <NodeActionsHint />}

                <WorkflowEditorToolbar
                    enableUndoRedo={enableUndoRedo}
                    onAddStickyNote={readOnlyWorkflow ? undefined : handleAddStickyNote}
                    readOnly={!!readOnlyWorkflow}
                />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
