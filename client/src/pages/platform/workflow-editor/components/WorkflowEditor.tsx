import '@xyflow/react/dist/base.css';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CANVAS_BACKGROUND_COLOR} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {Background, BackgroundVariant, ReactFlow} from '@xyflow/react';
import {useEffect} from 'react';
import {useShallow} from 'zustand/react/shallow';

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
        handleNodeDragStart,
        handleNodeDragStop,
        handleNodesChange,
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

    return (
        <div className="flex h-full flex-1 flex-col rounded-lg bg-background">
            <ReactFlow
                deleteKeyCode={null}
                edgeTypes={edgeTypes}
                edges={edges}
                maxZoom={1.5}
                minZoom={0.001}
                nodeTypes={nodeTypes}
                nodes={nodes}
                nodesConnectable={false}
                nodesDraggable={!readOnlyWorkflow && !nodesLocked}
                onDragOver={onDragOver}
                onDrop={onDrop}
                onEdgesChange={onEdgesChange}
                onNodeDragStart={handleNodeDragStart}
                onNodeDragStop={handleNodeDragStop}
                onNodesChange={handleNodesChange}
                panActivationKeyCode={null}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

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
