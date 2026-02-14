import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CANVAS_BACKGROUND_COLOR} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {Background, BackgroundVariant, ControlButton, Controls, ReactFlow} from '@xyflow/react';
import {ArrowDownIcon, ArrowRightIcon, BrushCleaningIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorCanvas from '../hooks/useWorkflowEditorCanvas';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';

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
    invalidateWorkflowQueries: () => void;
    projectLeftSidebarOpen?: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
};

const WorkflowEditor = ({
    componentDefinitions,
    customCanvasWidth,
    invalidateWorkflowQueries,
    projectLeftSidebarOpen,
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
    const {layoutDirection, setLayoutDirection} = useLayoutDirectionStore(
        useShallow((state) => ({
            layoutDirection: state.layoutDirection,
            setLayoutDirection: state.setLayoutDirection,
        }))
    );

    const {
        edgeTypes,
        handleNodeDragStart,
        handleNodeDragStop,
        handleNodesChange,
        handleResetLayout,
        nodeTypes,
        onDragOver,
        onDrop,
    } = useWorkflowEditorCanvas({
        componentDefinitions,
        customCanvasWidth,
        invalidateWorkflowQueries,
        projectLeftSidebarOpen,
        readOnlyWorkflow,
        taskDispatcherDefinitions,
    });

    return (
        <div className="flex h-full flex-1 flex-col rounded-lg bg-background">
            <ReactFlow
                edgeTypes={edgeTypes}
                edges={edges}
                maxZoom={1.5}
                minZoom={0.001}
                nodeTypes={nodeTypes}
                nodes={nodes}
                nodesConnectable={false}
                nodesDraggable
                onDragOver={onDragOver}
                onDrop={onDrop}
                onEdgesChange={onEdgesChange}
                onNodeDragStart={handleNodeDragStart}
                onNodeDragStop={handleNodeDragStop}
                onNodesChange={handleNodesChange}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

                <Controls
                    className="m-2 mb-3 rounded-md border border-stroke-neutral-secondary bg-background"
                    fitViewOptions={{duration: 500, minZoom: 0.2}}
                    showInteractive={false}
                >
                    <ControlButton
                        onClick={() => setLayoutDirection(layoutDirection === 'TB' ? 'LR' : 'TB')}
                        title={layoutDirection === 'TB' ? 'Switch to horizontal layout' : 'Switch to vertical layout'}
                    >
                        {layoutDirection === 'TB' ? (
                            <ArrowRightIcon className="size-3" />
                        ) : (
                            <ArrowDownIcon className="size-3" />
                        )}
                    </ControlButton>

                    <ControlButton onClick={handleResetLayout} title="Reset layout">
                        <BrushCleaningIcon className="size-3" />
                    </ControlButton>
                </Controls>
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
