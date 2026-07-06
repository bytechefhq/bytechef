import '@xyflow/react/dist/base.css';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CANVAS_BACKGROUND_COLOR} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {Background, BackgroundVariant, ReactFlow} from '@xyflow/react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorCanvas from '../hooks/useWorkflowEditorCanvas';
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
    leftSidebarOpen?: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
};

const WorkflowEditor = ({
    componentDefinitions,
    customCanvasWidth,
    enableUndoRedo,
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

    const {edgeTypes, handleNodeDragStart, handleNodeDragStop, handleNodesChange, nodeTypes, onDragOver, onDrop} =
        useWorkflowEditorCanvas({
            componentDefinitions,
            customCanvasWidth,
            leftSidebarOpen,
            readOnlyWorkflow,
            taskDispatcherDefinitions,
        });

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
                nodesDraggable={!readOnlyWorkflow}
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

                <WorkflowEditorToolbar enableUndoRedo={enableUndoRedo} readOnly={!!readOnlyWorkflow} />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
