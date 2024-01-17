import {Button} from '@/components/ui/button';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowNodesPopoverMenu from '@/pages/automation/project/components/WorkflowNodesPopoverMenu';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {WorkflowDefinition} from '@/types/types';
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

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {componentNames, projectId, setComponentNames, setWorkflow, workflow} = useWorkflowDataStore();

    const handleNodeClick = useNodeClickHandler(data, id);

    const {getEdges, getNode, getNodes, setEdges, setNodes} = useReactFlow();

    const isSelected = currentNode.name === data.name;

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projectWorkflows(projectId!)});
        },
    });

    const handleDeleteActionNodeClick = () => {
        const nodes = getNodes();
        const node = getNode(id);

        if (!node) {
            return;
        }

        const edges = getEdges();

        setNodes((nodes) => nodes.filter((node) => node.id !== id));

        const connectedEdges = getConnectedEdges([node], edges);
        const currentNodeIndex = nodes.findIndex((node) => node.id === id);

        const previousNode = nodes[currentNodeIndex - 1];
        const nextNode = nodes[currentNodeIndex + 1];

        if (previousNode && nextNode) {
            const connectedEdgeIds = connectedEdges.map((edge) => edge.id);

            setEdges((edges) => {
                const leftoverEdges = edges.filter((edge) => !connectedEdgeIds.includes(edge.id));

                if (previousNode.type === 'workflow' && nextNode.type === 'placeholder') {
                    return [
                        ...leftoverEdges,
                        {
                            id: `${previousNode.id}=>${nextNode.id}`,
                            source: previousNode.id,
                            target: nextNode.id,
                            type: 'placeholder',
                        },
                    ];
                }

                return [
                    ...leftoverEdges,
                    {
                        id: `${previousNode.id}=>${nextNode.id}`,
                        source: previousNode.id,
                        target: nextNode.id,
                        type: 'workflow',
                    },
                ];
            });
        }

        setComponentNames(componentNames.filter((componentName) => componentName !== data.name));

        if (!workflow?.definition) {
            return;
        }

        const workflowDefinition: WorkflowDefinition = JSON.parse(workflow?.definition);

        const updatedTasks = workflowDefinition!.tasks?.filter((task) => task.name !== data.name);

        setWorkflow({
            ...workflow,
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
    };

    return (
        <div
            className="relative flex min-w-[240px] cursor-pointer items-center justify-center"
            onMouseOut={() => setIsHovered(false)}
            onMouseOver={() => setIsHovered(true)}
        >
            {isHovered && (
                <div className="absolute left-[-40px] pr-4">
                    {data.type === 'trigger' ? (
                        <WorkflowNodesPopoverMenu hideActionComponents hideTaskDispatchers id={id}>
                            <Button
                                className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                                title="Edit a trigger"
                                variant="outline"
                            >
                                <PencilIcon className="h-4 w-4" />
                            </Button>
                        </WorkflowNodesPopoverMenu>
                    ) : (
                        <Button
                            className="bg-white p-2 shadow-md hover:text-red-500 hover:shadow-sm"
                            onClick={handleDeleteActionNodeClick}
                            title="Delete a node"
                            variant="outline"
                        >
                            <TrashIcon className="h-4 w-4" />
                        </Button>
                    )}
                </div>
            )}

            <Button
                className={twMerge(
                    'h-18 w-18 rounded-md border-2 border-gray-300 bg-white p-4 shadow hover:border-blue-200 hover:bg-blue-200 hover:shadow-none',
                    isSelected && workflowNodeDetailsPanelOpen && 'border-blue-300 bg-blue-100 shadow-none'
                )}
                onClick={handleNodeClick}
            >
                {data.icon}
            </Button>

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="text-sm">{data.title || data.label}</span>

                <span className="text-xs text-gray-500">{data.name}</span>
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
