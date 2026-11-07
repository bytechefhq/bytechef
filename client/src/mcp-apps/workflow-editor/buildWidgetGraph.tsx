import useLayoutEngineStore from '@/pages/platform/workflow-editor/stores/useLayoutEngineStore';
import buildWorkflowGraph from '@/pages/platform/workflow-editor/utils/buildWorkflowGraph';
import {getElkLayoutElements} from '@/pages/platform/workflow-editor/utils/elkLayoutUtils';
import removeTrailingBranchPlaceholders from '@/pages/platform/workflow-editor/utils/removeTrailingBranchPlaceholders';
import {forEachNestedTaskGroup} from '@/pages/platform/workflow-editor/utils/taskTraversalUtils';
import {FINAL_PLACEHOLDER_NODE_ID, LayoutDirectionType, TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import ComponentImage from './ComponentImage';
import {buildIconUrl} from './iconUrl';

const NODE_TYPE_MAPPING: Record<string, string> = {
    clusterRoot: 'readonly',
    placeholder: 'readonlyPlaceholder',
    workflow: 'readonly',
};

export interface WorkflowDefinitionI {
    label?: string;
    tasks?: WorkflowTask[];
    triggers?: WorkflowTrigger[];
}

export interface WidgetGraphI {
    edges: Edge[];
    nodes: Node[];
}

interface LayoutWidgetGraphProps {
    canvasWidth: number;
    direction: LayoutDirectionType;
    workflowDefinition: WorkflowDefinitionI;
}

function withComponentIcon(node: Node): Node {
    const nodeData = node.data as NodeDataType;

    if (!nodeData.componentName) {
        return node;
    }

    return {
        ...node,
        data: {
            ...nodeData,
            icon: (
                <ComponentImage
                    alt={nodeData.componentName}
                    className="size-9 max-w-none shrink-0"
                    size={36}
                    src={buildIconUrl(nodeData.componentName)}
                />
            ),
        },
    };
}

export function flattenWorkflowTasks(tasks: WorkflowTask[] | undefined): WorkflowTask[] {
    const flattenedTasks: WorkflowTask[] = [];

    for (const task of tasks ?? []) {
        flattenedTasks.push(task);

        if (task.parameters) {
            forEachNestedTaskGroup(task.parameters, (nestedTasks) => {
                flattenedTasks.push(...flattenWorkflowTasks(nestedTasks));
            });
        }
    }

    return flattenedTasks;
}

export function buildWidgetGraph(workflowDefinition: WorkflowDefinitionI, canvasWidth: number): WidgetGraphI {
    const graph = buildWorkflowGraph({
        canvasWidth,
        componentDefinitions: [],
        taskDispatcherDefinitions: [],
        tasks: flattenWorkflowTasks(workflowDefinition.tasks),
        triggers: workflowDefinition.triggers,
    });

    const nodes = graph.nodes
        .filter((node) => node.id !== TRIGGER_PLACEHOLDER_NODE_ID && node.id !== FINAL_PLACEHOLDER_NODE_ID)
        .map((node) => withComponentIcon({...node, type: NODE_TYPE_MAPPING[node.type ?? ''] ?? node.type}));

    const nodeIds = new Set(nodes.map((node) => node.id));

    const edges = graph.edges
        .filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target))
        .map((edge) => ({...edge, type: 'smoothstep'}));

    return removeTrailingBranchPlaceholders(nodes, edges);
}

export async function layoutWidgetGraph({
    canvasWidth,
    direction,
    workflowDefinition,
}: LayoutWidgetGraphProps): Promise<WidgetGraphI> {
    const {edges, nodes} = buildWidgetGraph(workflowDefinition, canvasWidth);

    if (nodes.length === 0) {
        return {edges: [], nodes: []};
    }

    const layoutResult = await getElkLayoutElements({canvasWidth, direction, edges, nodes});

    useLayoutEngineStore.getState().setLastAppliedLayoutEngine('elk');

    return {edges: layoutResult.edges, nodes: layoutResult.nodes};
}
