import defaultNodes from '@/shared/defaultNodes';
import {WorkflowTaskModel} from '@/shared/middleware/automation/configuration';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
} from '@/shared/middleware/platform/configuration';
import {getRandomId} from '@/shared/util/random-utils';
import {stratify, tree} from 'd3-hierarchy';
import {ComponentIcon} from 'lucide-react';
import {useEffect} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, Node, ReactFlowState, useReactFlow, useStore} from 'reactflow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

const TASK_DISPATCHER_NAMES = [
    'branch',
    'condition',
    'each',
    'fork-join',
    'loop',
    'loop-break',
    'map',
    'parallel',
    'subflow',
];

const PLACEHOLDER_NODE_ID = getRandomId();

// initialize the tree layout (see https://observablehq.com/@d3/tree for examples)
const layout = tree<Node>()
    // the node size configures the spacing between the nodes ([width, height])
    .nodeSize([200, 150])
    // this is needed for creating equal space between all nodes
    .separation(() => 1);

// the layouting function
// accepts current nodes and edges and returns the layouted nodes with their updated positions
function layoutNodes(nodes: Node[], edges: Edge[]): Node[] {
    // convert nodes and edges into a hierarchical object for using it with the layout function
    const hierarchy = stratify<Node>()
        .id((data) => data.id)
        // get the id of each node by searching through the edges
        // this only works if every node has one connection
        .parentId((node: Node) => edges.find((edge: Edge) => edge.target === node.id)?.source)(nodes);

    // run the layout algorithm with the hierarchy data structure
    const root = layout(hierarchy);

    const descendants = root.descendants();

    // convert the hierarchy back to react flow nodes (the original node is stored as d.data)
    // we only extract the position from the d3 function
    return descendants.map((descendant) => ({
        ...descendant.data,
        position: {
            x: descendant.parent ? descendant.parent.x : descendant.x,
            y: descendant.y,
        },
    }));
}

// this is the store selector that is used for triggering the layout, this returns the number of nodes once they change
const nodeCountSelector = (state: ReactFlowState) => state.nodeInternals.size;

const convertTaskToNode = (
    task: WorkflowTaskModel,
    taskDefinition: ComponentDefinitionBasicModel | TaskDispatcherDefinitionBasicModel,
    index: number
): Node => {
    const componentName = task.type.split('/')[0];

    return {
        data: {
            ...task,
            componentName,
            icon: (
                <InlineSVG
                    className="size-9"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={taskDefinition.icon!}
                />
            ),
            operationName: task.type.split('/')[2],
            taskDispatcher: TASK_DISPATCHER_NAMES.includes(componentName),
            trigger: index === 0,
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: index * 150},
        type: 'workflow',
    };
};

export default function useLayout({
    componentDefinitions,
    taskDispatcherDefinitions,
}: {
    componentDefinitions: Array<ComponentDefinitionBasicModel>;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionBasicModel>;
}) {
    const nodeCount = useStore(nodeCountSelector);

    const {getEdges, getNodes, setEdges, setNodes} = useReactFlow();

    const {
        workflow: {componentNames, tasks, triggers},
    } = useWorkflowDataStore();

    const triggerComponentName = triggers?.[0]?.type.split('/')[0];

    const triggerDefinition = componentDefinitions.find((definition) => definition.name === triggerComponentName);

    const triggerNode =
        triggerDefinition && triggers?.[0] ? convertTaskToNode(triggers[0], triggerDefinition, 0) : defaultNodes[0];

    let taskNodes: Array<Node> = [];

    if (tasks) {
        taskNodes = tasks?.map((task, index) => {
            const componentName = task.type.split('/')[0];

            const combinedDefinitions = [...componentDefinitions, ...taskDispatcherDefinitions];

            const taskDefinition = combinedDefinitions.find((definition) => definition.name === componentName);

            if (taskDefinition) {
                return convertTaskToNode(task, taskDefinition, index + 1);
            } else {
                return {
                    data: {
                        ...task,
                        componentName,
                        icon: <ComponentIcon className="size-9 flex-none text-gray-900" />,
                        operationName: task.type.split('/')[2],
                        taskDispatcher: TASK_DISPATCHER_NAMES.includes(componentName),
                        trigger: index === 0,
                    },
                    id: task.name,
                    position: {x: 0, y: index * 150},
                    type: 'workflow',
                };
            }
        });
    }

    const triggerAndTaskNodes: Array<Node> = [triggerNode, ...(taskNodes?.length ? taskNodes : [])];

    const placeholderNode: Node = {
        data: {label: '+'},
        id: PLACEHOLDER_NODE_ID,
        position: {x: 0, y: (triggerAndTaskNodes.length || 1) * 150},
        type: 'placeholder',
    };

    const taskEdges: Array<Edge> = triggerAndTaskNodes.map((taskNode, index) => {
        const nextNode = triggerAndTaskNodes[index + 1];

        if (nextNode) {
            return {
                id: `${taskNode.id}=>${nextNode.id}`,
                source: taskNode.id,
                target: nextNode.id,
                type: 'workflow',
            };
        } else {
            return {
                id: `${taskNode.id}=>${PLACEHOLDER_NODE_ID}`,
                source: taskNode.id,
                target: PLACEHOLDER_NODE_ID,
                type: 'placeholder',
            };
        }
    });

    useEffect(() => {
        if (triggerAndTaskNodes.length) {
            const allNodes: Array<Node> = taskNodes?.length
                ? [...triggerAndTaskNodes, placeholderNode]
                : [triggerNode, placeholderNode];

            layoutNodes(allNodes, taskEdges);

            setNodes(allNodes);
            setEdges(taskEdges);

            return;
        }

        const nodes = getNodes();
        const edges = getEdges();

        const targetNodes = layoutNodes(nodes, edges);

        setNodes(targetNodes);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [nodeCount, getEdges, getNodes, setNodes, triggerAndTaskNodes, componentNames]);
}
