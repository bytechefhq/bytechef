import '@xyflow/react/dist/base.css';
import ReadOnlyNode from '@/pages/platform/workflow-editor/nodes/ReadOnlyNode';
import ReadOnlyPlaceholderNode from '@/pages/platform/workflow-editor/nodes/ReadOnlyPlaceholderNode';
import {WorkflowChangeHighlightContext} from '@/pages/platform/workflow-editor/providers/workflowChangeHighlightContext';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import useLayoutEngineStore from '@/pages/platform/workflow-editor/stores/useLayoutEngineStore';
import buildWorkflowGraph from '@/pages/platform/workflow-editor/utils/buildWorkflowGraph';
import {getElkLayoutElements} from '@/pages/platform/workflow-editor/utils/elkLayoutUtils';
import isElkLayoutSupported from '@/pages/platform/workflow-editor/utils/isElkLayoutSupported';
import {getLayoutElements} from '@/pages/platform/workflow-editor/utils/layoutUtils';
import {CANVAS_BACKGROUND_COLOR, FINAL_PLACEHOLDER_NODE_ID, TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {WorkflowNodeChangeKind} from '@/shared/middleware/graphql';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {Background, BackgroundVariant, Edge, Node, ReactFlow, ReactFlowProvider, useReactFlow} from '@xyflow/react';
import {useEffect, useRef, useState} from 'react';

import ChangeCanvasEdge from './ChangeCanvasEdge';
import {
    ChangeCanvasBottomGhostNode,
    ChangeCanvasLeftGhostNode,
    ChangeCanvasTopGhostNode,
} from './ChangeCanvasGhostNodes';

const DEFAULT_CANVAS_WIDTH = 756;

const FIT_VIEW_OPTIONS = {maxZoom: 1, minZoom: 0.1, padding: 0.15};

const EDGE_TYPES = {smoothstep: ChangeCanvasEdge};

const NODE_TYPES = {
    readonly: ReadOnlyNode,
    readonlyPlaceholder: ReadOnlyPlaceholderNode,
    taskDispatcherBottomGhostNode: ChangeCanvasBottomGhostNode,
    taskDispatcherLeftGhostNode: ChangeCanvasLeftGhostNode,
    taskDispatcherTopGhostNode: ChangeCanvasTopGhostNode,
};

const NODE_TYPE_MAPPING: Record<string, string> = {
    clusterRoot: 'readonly',
    placeholder: 'readonlyPlaceholder',
    workflow: 'readonly',
};

interface FitViewOnLayoutProps {
    nodes: Node[];
}

/**
 * The `fitView` prop fits only the first nodes; the layout reruns once the canvas width is measured, so the view is
 * refitted after every layout to keep the graph centered.
 */
const FitViewOnLayout = ({nodes}: FitViewOnLayoutProps) => {
    const {fitView} = useReactFlow();

    useEffect(() => {
        if (nodes.length > 0) {
            fitView(FIT_VIEW_OPTIONS);
        }
    }, [fitView, nodes]);

    return null;
};

interface WorkflowChangeCanvasProps {
    componentDefinitions: ComponentDefinitionBasic[];
    highlights: Record<string, WorkflowNodeChangeKind>;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    workflow: Workflow;
}

const WorkflowChangeCanvas = ({
    componentDefinitions,
    highlights,
    taskDispatcherDefinitions,
    workflow,
}: WorkflowChangeCanvasProps) => {
    const [canvasWidth, setCanvasWidth] = useState(DEFAULT_CANVAS_WIDTH);
    const [elements, setElements] = useState<{edges: Edge[]; nodes: Node[]}>({edges: [], nodes: []});

    const containerRef = useRef<HTMLDivElement>(null);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const layoutEngine = useLayoutEngineStore((state) => state.layoutEngine);

    useEffect(() => {
        const container = containerRef.current;

        if (!container || typeof ResizeObserver === 'undefined') {
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

        const layoutGraph = async () => {
            const graph = buildWorkflowGraph({
                canvasWidth,
                componentDefinitions,
                taskDispatcherDefinitions,
                tasks: workflow.tasks,
                triggers: workflow.triggers,
            });

            const nodes = graph.nodes
                .filter((node) => node.id !== TRIGGER_PLACEHOLDER_NODE_ID && node.id !== FINAL_PLACEHOLDER_NODE_ID)
                .map((node) => ({...node, type: NODE_TYPE_MAPPING[node.type ?? ''] ?? node.type}));
            const nodeIds = new Set(nodes.map((node) => node.id));
            const edges = graph.edges
                .filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target))
                .map((edge) => ({...edge, type: 'smoothstep'}));

            if (nodes.length === 0) {
                if (!cancelled) {
                    setElements({edges: [], nodes: []});
                }

                return;
            }

            try {
                const layoutFunction =
                    layoutEngine === 'elk' && isElkLayoutSupported(nodes) ? getElkLayoutElements : getLayoutElements;

                const layoutResult = await layoutFunction({canvasWidth, direction: layoutDirection, edges, nodes});

                if (!cancelled) {
                    setElements({edges: layoutResult.edges, nodes: layoutResult.nodes});
                }
            } catch {
                if (!cancelled) {
                    setElements({edges: [], nodes: []});
                }
            }
        };

        layoutGraph();

        return () => {
            cancelled = true;
        };
    }, [canvasWidth, componentDefinitions, layoutDirection, layoutEngine, taskDispatcherDefinitions, workflow]);

    return (
        <WorkflowEditorReadOnlyContext.Provider value={true}>
            <WorkflowChangeHighlightContext.Provider value={highlights}>
                <div className="size-full rounded-lg bg-background" ref={containerRef}>
                    <ReactFlowProvider>
                        <ReactFlow
                            edgeTypes={EDGE_TYPES}
                            edges={elements.edges}
                            elementsSelectable={false}
                            fitView
                            fitViewOptions={FIT_VIEW_OPTIONS}
                            maxZoom={1.5}
                            minZoom={0.001}
                            nodeTypes={NODE_TYPES}
                            nodes={elements.nodes}
                            nodesConnectable={false}
                            nodesDraggable={false}
                            proOptions={{hideAttribution: true}}
                        >
                            <FitViewOnLayout nodes={elements.nodes} />

                            <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />
                        </ReactFlow>
                    </ReactFlowProvider>
                </div>
            </WorkflowChangeHighlightContext.Provider>
        </WorkflowEditorReadOnlyContext.Provider>
    );
};

export default WorkflowChangeCanvas;
