import ReadOnlyNode from '@/pages/platform/workflow-editor/nodes/ReadOnlyNode';
import ReadOnlyPlaceholderNode from '@/pages/platform/workflow-editor/nodes/ReadOnlyPlaceholderNode';
import TaskDispatcherBottomGhostNode from '@/pages/platform/workflow-editor/nodes/TaskDispatcherBottomGhostNode';
import TaskDispatcherLeftGhostNode from '@/pages/platform/workflow-editor/nodes/TaskDispatcherLeftGhostNode';
import TaskDispatcherTopGhostNode from '@/pages/platform/workflow-editor/nodes/TaskDispatcherTopGhostNode';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import ChangeCanvasEdge from '@/shared/components/version-workflow-changes/ChangeCanvasEdge';
import {CANVAS_BACKGROUND_COLOR} from '@/shared/constants';
import {Background, BackgroundVariant, ReactFlow, ReactFlowProvider, useReactFlow} from '@xyflow/react';
import {LoaderCircleIcon} from 'lucide-react';
import {type RefObject, useEffect, useRef, useState} from 'react';

import '@xyflow/react/dist/base.css';

import WorkflowGraphControls from './WorkflowGraphControls';
import {WidgetGraphI, WorkflowDefinitionI, layoutWidgetGraph} from './buildWidgetGraph';

const DEFAULT_CANVAS_WIDTH = 756;

const EDGE_TYPES = {smoothstep: ChangeCanvasEdge};

const FIT_VIEW_OPTIONS = {maxZoom: 1, padding: 0.2};

const NODE_TYPES = {
    readonly: ReadOnlyNode,
    readonlyPlaceholder: ReadOnlyPlaceholderNode,
    taskDispatcherBottomGhostNode: TaskDispatcherBottomGhostNode,
    taskDispatcherLeftGhostNode: TaskDispatcherLeftGhostNode,
    taskDispatcherTopGhostNode: TaskDispatcherTopGhostNode,
};

type LayoutStatusType = 'loading' | 'ready' | 'error';

interface FitViewOnChangeProps {
    containerRef: RefObject<HTMLDivElement | null>;
    graph: WidgetGraphI;
}

function FitViewOnChange({containerRef, graph}: FitViewOnChangeProps) {
    const {fitView} = useReactFlow();

    useEffect(() => {
        if (graph.nodes.length > 0) {
            void fitView(FIT_VIEW_OPTIONS);
        }
    }, [fitView, graph]);

    useEffect(() => {
        const container = containerRef.current;

        if (!container) {
            return;
        }

        const resizeObserver = new ResizeObserver(() => {
            void fitView(FIT_VIEW_OPTIONS);
        });

        resizeObserver.observe(container);

        return () => resizeObserver.disconnect();
    }, [containerRef, fitView]);

    return null;
}

interface WorkflowGraphProps {
    workflowDefinition: WorkflowDefinitionI;
}

export default function WorkflowGraph({workflowDefinition}: WorkflowGraphProps) {
    const [canvasWidth, setCanvasWidth] = useState(DEFAULT_CANVAS_WIDTH);
    const [graph, setGraph] = useState<WidgetGraphI>({edges: [], nodes: []});
    const [status, setStatus] = useState<LayoutStatusType>('loading');

    const containerRef = useRef<HTMLDivElement>(null);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const resetLayoutDirection = useLayoutDirectionStore((state) => state.resetLayoutDirection);

    useEffect(() => {
        const container = containerRef.current;

        if (!container) {
            return;
        }

        const resizeObserver = new ResizeObserver((entries) => {
            const width = entries[0]?.contentRect.width;

            if (width) {
                setCanvasWidth(width);
            }
        });

        resizeObserver.observe(container);

        return () => resizeObserver.disconnect();
    }, []);

    useEffect(() => {
        let cancelled = false;

        layoutWidgetGraph({canvasWidth, direction: layoutDirection, workflowDefinition})
            .then((layoutedGraph) => {
                if (!cancelled) {
                    setGraph(layoutedGraph);
                    setStatus(layoutedGraph.nodes.length > 0 ? 'ready' : 'error');
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setStatus('error');
                }
            });

        return () => {
            cancelled = true;
        };
    }, [canvasWidth, layoutDirection, workflowDefinition]);

    return (
        <WorkflowEditorReadOnlyContext.Provider value={true}>
            <div className="relative size-full" ref={containerRef}>
                {status === 'loading' && (
                    <div className="flex size-full items-center justify-center">
                        <LoaderCircleIcon
                            aria-label="Loading workflow"
                            className="animate-spin text-content-neutral-secondary"
                            size={28}
                        />
                    </div>
                )}

                {status === 'error' && (
                    <div className="flex size-full items-center justify-center text-content-neutral-secondary">
                        Workflow preview unavailable
                    </div>
                )}

                {status === 'ready' && (
                    <ReactFlowProvider>
                        <ReactFlow
                            edgeTypes={EDGE_TYPES}
                            edges={graph.edges}
                            elementsSelectable={false}
                            fitView
                            fitViewOptions={FIT_VIEW_OPTIONS}
                            minZoom={0.1}
                            nodeTypes={NODE_TYPES}
                            nodes={graph.nodes}
                            nodesConnectable={false}
                            nodesDraggable={false}
                            panOnScroll={false}
                            proOptions={{hideAttribution: true}}
                        >
                            <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

                            <FitViewOnChange containerRef={containerRef} graph={graph} />

                            <WorkflowGraphControls
                                direction={layoutDirection}
                                onToggleDirection={() => resetLayoutDirection(layoutDirection === 'LR' ? 'TB' : 'LR')}
                            />
                        </ReactFlow>
                    </ReactFlowProvider>
                )}
            </div>
        </WorkflowEditorReadOnlyContext.Provider>
    );
}
