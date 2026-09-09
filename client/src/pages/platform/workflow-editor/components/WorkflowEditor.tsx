import '@xyflow/react/dist/base.css';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CANVAS_BACKGROUND_COLOR, CANVAS_TOP_OFFSET} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {Background, BackgroundVariant, ReactFlow, useNodesInitialized, useReactFlow} from '@xyflow/react';
import {useEffect} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorCanvas from '../hooks/useWorkflowEditorCanvas';
import {WorkflowEditorReadOnlyContext} from '../providers/workflowEditorReadOnlyContext';
import NodeActionsHint from './NodeActionsHint';
import WorkflowEditorToolbar from './WorkflowEditorToolbar';
import WorkflowIssuesNote from './WorkflowIssuesNote';

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
    className?: string;
    componentDefinitions: ComponentDefinitionBasic[];
    customCanvasWidth?: number;
    enableUndoRedo?: boolean;
    fitViewOnLoad?: boolean;
    leftSidebarOpen?: boolean;
    onFitView?: () => void;
    preview?: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
};

const CANVAS_DEFAULT_VIEWPORT = {x: 0, y: CANVAS_TOP_OFFSET, zoom: 1};

const WorkflowEditor = ({
    className,
    componentDefinitions,
    customCanvasWidth,
    enableUndoRedo,
    fitViewOnLoad,
    leftSidebarOpen,
    onFitView,
    preview,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: WorkflowEditorPropsType & ConditionalWorkflowEditorPropsType) => {
    const fitsViewOnLoad = fitViewOnLoad || preview;

    const {fitView} = useReactFlow();
    const nodesInitialized = useNodesInitialized();
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
            fitViewOnLoad: fitsViewOnLoad,
            leftSidebarOpen,
            readOnlyWorkflow,
            taskDispatcherDefinitions,
        });

    useEffect(() => {
        if (!fitsViewOnLoad || !nodesInitialized) {
            return;
        }

        fitView({duration: 0, maxZoom: 1, minZoom: 0.1, padding: 0.15});

        onFitView?.();
    }, [fitsViewOnLoad, fitView, nodes, nodesInitialized, onFitView]);

    return (
        <WorkflowEditorReadOnlyContext.Provider value={!!readOnlyWorkflow}>
            <div className={twMerge('flex h-full flex-1 flex-col rounded-lg bg-background', className)}>
                <ReactFlow
                    defaultViewport={CANVAS_DEFAULT_VIEWPORT}
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
                    panOnDrag={!preview}
                    panOnScroll={!preview}
                    proOptions={{hideAttribution: true}}
                    zoomOnDoubleClick={false}
                    zoomOnPinch={!preview}
                    zoomOnScroll={false}
                >
                    <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

                    {!readOnlyWorkflow && nodes.length > 0 && <WorkflowIssuesNote fallback={<NodeActionsHint />} />}

                    {!preview && (
                        <WorkflowEditorToolbar enableUndoRedo={enableUndoRedo} readOnly={!!readOnlyWorkflow} />
                    )}
                </ReactFlow>
            </div>
        </WorkflowEditorReadOnlyContext.Provider>
    );
};

export default WorkflowEditor;
