import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {HoverCard} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, TrashIcon} from 'lucide-react';
import {memo, useState} from 'react';
import {Handle, NodeProps, Position, useReactFlow} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '../utils/handleDeleteTask';
import styles from './NodeTypes.module.css';

const WorkflowNode = ({data, id}: NodeProps) => {
    const [isHovered, setIsHovered] = useState(false);
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();

    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames} = workflow;

    const handleNodeClick = useNodeClickHandler(data, id);

    const {getNode} = useReactFlow();

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

    const handleDeleteNodeClick = () =>
        handleDeleteTask({
            componentNames,
            currentComponent,
            currentNode,
            data,
            getNode,
            id,
            queryClient,
            setCurrentComponent,
            setCurrentNode,
            setWorkflow,
            updateWorkflowMutation,
            workflow,
        });

    return (
        <div
            className="relative flex min-w-[240px] cursor-pointer items-center justify-center"
            data-nodeType={data.trigger ? 'trigger' : 'task'}
            onMouseOut={() => setIsHovered(false)}
            onMouseOver={() => setIsHovered(true)}
        >
            {isHovered && (
                <div className="absolute left-[-40px] pr-4">
                    {data.trigger ? (
                        <WorkflowNodesPopoverMenu hideActionComponents hideTaskDispatchers sourceNodeId={id}>
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
                        setHoveredNodeName(data.name);
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

                <span className="text-sm text-gray-500">{data.workflowNodeName}</span>
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

            {data.name.includes('condition') && (
                <div className="absolute bottom-0 left-0 font-bold text-muted-foreground">
                    <span className="absolute -bottom-6 -left-32">TRUE</span>

                    <span className="absolute -bottom-6 left-40">FALSE</span>
                </div>
            )}
        </div>
    );
};

export default memo(WorkflowNode);
