import {Background, BackgroundVariant, type EdgeTypes, type NodeTypes, ReactFlow, ReactFlowProvider} from '@xyflow/react';
import {LoaderCircleIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import {WorkflowDefinitionType} from '../types';
import WorkflowGraphControls from './WorkflowGraphControls';
import LabeledBranchCaseEdge from './edges/LabeledBranchCaseEdge';
import RoundedSmoothStepEdge from './edges/RoundedSmoothStepEdge';
import {LAYOUT_DIRECTION, LayoutDirectionType} from './layout/constants';
import ReadOnlyNode from './nodes/ReadOnlyNode';
import ReadOnlyPlaceholderNode from './nodes/ReadOnlyPlaceholderNode';
import TaskDispatcherBottomGhostNode from './nodes/TaskDispatcherBottomGhostNode';
import TaskDispatcherLeftGhostNode from './nodes/TaskDispatcherLeftGhostNode';
import TaskDispatcherTopGhostNode from './nodes/TaskDispatcherTopGhostNode';
import {LayoutDirectionContext} from './useLayoutDirection';
import {useWorkflowGraphLayout} from './useWorkflowGraphLayout';

import '@xyflow/react/dist/base.css';

interface WorkflowGraphProps {
    workflowDefinition: WorkflowDefinitionType;
}

export default function WorkflowGraph({workflowDefinition}: WorkflowGraphProps) {
    const [direction, setDirection] = useState<LayoutDirectionType>(LAYOUT_DIRECTION);

    const {edges, nodes, status} = useWorkflowGraphLayout(workflowDefinition, direction);

    const nodeTypes = useMemo<NodeTypes>(
        () => ({
            readonly: ReadOnlyNode,
            readonlyPlaceholder: ReadOnlyPlaceholderNode,
            taskDispatcherBottomGhostNode: TaskDispatcherBottomGhostNode,
            taskDispatcherLeftGhostNode: TaskDispatcherLeftGhostNode,
            taskDispatcherTopGhostNode: TaskDispatcherTopGhostNode,
        }),
        []
    );

    const edgeTypes = useMemo<EdgeTypes>(
        () => ({labeledBranchCase: LabeledBranchCaseEdge, smoothstep: RoundedSmoothStepEdge}),
        []
    );

    if (status === 'loading') {
        return (
            <div className="flex size-full min-h-[30vh] items-center justify-center">
                <LoaderCircleIcon aria-label="Loading workflow" className="animate-spin text-content-neutral-secondary" size={28} />
            </div>
        );
    }

    if (status === 'error' || nodes.length === 0) {
        return (
            <div className="flex size-full min-h-[30vh] items-center justify-center text-content-neutral-secondary">
                Workflow preview unavailable
            </div>
        );
    }

    return (
        <div className="size-full">
            <LayoutDirectionContext.Provider value={direction}>
                <ReactFlowProvider>
                    <ReactFlow
                        edgeTypes={edgeTypes}
                        edges={edges}
                        fitView
                        key={direction}
                        minZoom={0.1}
                        nodeTypes={nodeTypes}
                        nodes={nodes}
                        nodesConnectable={false}
                        nodesDraggable={false}
                        panOnScroll={false}
                        proOptions={{hideAttribution: true}}
                    >
                        <Background variant={BackgroundVariant.Dots} />

                        <WorkflowGraphControls
                            direction={direction}
                            onToggleDirection={() => setDirection((current) => (current === 'LR' ? 'TB' : 'LR'))}
                        />
                    </ReactFlow>
                </ReactFlowProvider>
            </LayoutDirectionContext.Provider>
        </div>
    );
}
