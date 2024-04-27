import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {useUpdateWorkflowNodeOutputsMutation} from '@/mutations/platform/workflowNodeOutputs.mutations';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {useGetWorkflowNodeDescriptionQuery} from '@/queries/platform/workflowNodeDescriptions.queries';
import {WorkflowNodeOutputKeys} from '@/queries/platform/workflowNodeOutputs.queries';
import {WorkflowDefinitionType} from '@/types/types';
import {HoverCard} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, TrashIcon} from 'lucide-react';
import {memo, useState} from 'react';
import {Handle, NodeProps, Position, getConnectedEdges, useReactFlow} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import styles from './NodeTypes.module.css';

const SPACE = 4;

const WorkflowNode = ({data, id}: NodeProps) => {
    const [isHovered, setIsHovered] = useState(false);
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames} = workflow;

    const handleNodeClick = useNodeClickHandler(data, id);

    const {getEdges, getNode, getNodes, setEdges, setNodes} = useReactFlow();

    const isSelected = currentNode?.name === data.name;

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            id: workflow.id!,
            workflowNodeName: hoveredNodeName!,
        },
        hoveredNodeName !== undefined
    );

    const queryClient = useQueryClient();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const updateWorkflowNodeOutputsMutation = useUpdateWorkflowNodeOutputsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOutputKeys.filteredWorkflowNodeOutputs({
                    id: workflow.id!,
                    lastWorkflowNodeName: currentNode?.name,
                }),
            });
        },
    });

    const handleDeleteNodeClick = () => {
        const nodes = getNodes();
        const node = getNode(id);

        if (!node) {
            return;
        }

        const edges = getEdges();

        const remainingNodes = nodes.filter((node) => node.id !== id);

        setNodes(remainingNodes);

        const connectedEdges = getConnectedEdges([node], edges);
        const deletedNodeIndex = nodes.findIndex((node) => node.id === id);

        const previousNode = nodes[deletedNodeIndex - 1];
        const nextNode = nodes[deletedNodeIndex + 1];

        if (previousNode && nextNode) {
            const connectedEdgeIds = connectedEdges.map((edge) => edge.id);

            setEdges((edges) => {
                const leftoverEdges = edges.filter((edge) => !connectedEdgeIds.includes(edge.id));

                const edgeType =
                    previousNode.type === 'workflow' && nextNode.type === 'placeholder' ? 'placeholder' : 'workflow';

                return [
                    ...leftoverEdges,
                    {
                        id: `${previousNode.id}=>${nextNode.id}`,
                        source: previousNode.id,
                        target: nextNode.id,
                        type: edgeType,
                    },
                ];
            });
        }

        if (!workflow?.definition) {
            return;
        }

        const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        const updatedTasks = workflowDefinition!.tasks?.filter((task) => task.name !== data.name);

        setWorkflow({
            ...workflow,
            componentNames: componentNames.filter((componentName) => componentName !== data.componentName),
            tasks: updatedTasks,
        });

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        tasks: updatedTasks,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        });

        updateWorkflowNodeOutputsMutation.mutate({
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.name,
        });
    };

    return (
        <div
            className="relative flex min-w-[240px] cursor-pointer items-center justify-center"
            onMouseOut={() => setIsHovered(false)}
            onMouseOver={() => setIsHovered(true)}
        >
            {isHovered && (
                <div className="absolute left-[-40px] pr-4">
                    {data.trigger ? (
                        <WorkflowNodesPopoverMenu hideActionComponents hideTaskDispatchers id={id}>
                            <Button
                                className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                                title="Change trigger component"
                                variant="outline"
                            >
                                <PencilIcon className="size-4" />
                            </Button>
                        </WorkflowNodesPopoverMenu>
                    ) : (
                        <Button
                            className="bg-white p-2 shadow-md hover:text-red-500 hover:shadow-sm"
                            onClick={handleDeleteNodeClick}
                            title="Delete a node"
                            variant="outline"
                        >
                            <TrashIcon className="size-4" />
                        </Button>
                    )}
                </div>
            )}

            <HoverCard
                onOpenChange={(open) => {
                    if (open) {
                        setHoveredNodeName(id);
                    } else {
                        setHoveredNodeName(undefined);
                    }
                }}
            >
                <HoverCardTrigger>
                    <Button
                        className={twMerge(
                            'h-18 w-18 rounded-md border-2 border-gray-300 bg-white p-4 shadow hover:border-blue-200 hover:bg-blue-200 hover:shadow-none',
                            isSelected && workflowNodeDetailsPanelOpen && 'border-blue-300 bg-blue-100 shadow-none'
                        )}
                        onClick={handleNodeClick}
                    >
                        {data.icon}
                    </Button>
                </HoverCardTrigger>

                <HoverCardContent className="text-sm" side="right">
                    {workflowNodeDescription?.description}
                </HoverCardContent>
            </HoverCard>

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="font-semibold">{data.title || data.label}</span>

                {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

                <span className="text-sm text-gray-500">{data.name}</span>
            </div>

            <Handle
                className={twMerge('left-[36px]', styles.handle)}
                isConnectable={false}
                position={Position.Top}
                type="target"
            />

            <Handle
                className={twMerge('left-[36px]', styles.handle)}
                isConnectable={false}
                position={Position.Bottom}
                type="source"
            />
        </div>
    );
};

export default memo(WorkflowNode);
