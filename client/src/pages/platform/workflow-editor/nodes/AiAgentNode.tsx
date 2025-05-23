import {Button} from '@/components/ui/button';
import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {NodeDataType} from '@/shared/types';
import {HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {TrashIcon} from 'lucide-react';
import {memo, useState} from 'react';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';

import useNodeClickHandler from '../hooks/useNodeClick';
import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '../utils/handleDeleteTask';
import styles from './NodeTypes.module.css';

const AiAgentNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isHovered, setIsHovered] = useState(false);
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {integrationId, projectId, workflow} = useWorkflowDataStore();

    const queryClient = useQueryClient();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const isSelected = currentNode?.name === data.name;

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            id: workflow.id!,
            workflowNodeName: hoveredNodeName!,
        },
        hoveredNodeName !== undefined
    );

    const handleNodeClick = useNodeClickHandler(data, id);

    const handleDeleteNodeClick = (data: NodeDataType) => {
        if (data) {
            handleDeleteTask({
                currentNode,
                data,
                integrationId,
                projectId,
                queryClient,
                updateWorkflowMutation,
                workflow,
            });
        }
    };

    return (
        <div
            className="nodrag relative flex min-w-60 cursor-pointer items-center justify-center"
            data-nodetype="aiAgentNode"
            key={id}
            onMouseOut={() => setIsHovered(false)}
            onMouseOver={() => setIsHovered(true)}
        >
            {isHovered && (
                <div className="absolute left-workflow-node-popover-hover pr-4">
                    <Button
                        className="bg-white p-2 shadow-md hover:text-red-500 hover:shadow-sm"
                        onClick={() => handleDeleteNodeClick(data)}
                        title="Delete a node"
                        variant="outline"
                    >
                        <TrashIcon className="size-4" />
                    </Button>
                </div>
            )}

            <HoverCard
                key={id}
                onOpenChange={(open) => {
                    if (open) {
                        setHoveredNodeName(data.name);
                    } else {
                        setHoveredNodeName(undefined);
                    }
                }}
            >
                <div
                    className={twMerge(
                        'size-18 flex cursor-pointer flex-col items-start gap-2 rounded-md border-2 border-gray-300 bg-background p-4 text-content-neutral-primary shadow hover:bg-background hover:shadow-none [&_svg]:size-8',
                        isSelected && workflowNodeDetailsPanelOpen && 'border-blue-300 bg-background shadow-none'
                    )}
                    onClick={handleNodeClick}
                >
                    <div className="flex w-full flex-1 items-center justify-between">
                        <HoverCardTrigger>{data.icon!}</HoverCardTrigger>
                    </div>
                </div>

                <HoverCardPortal>
                    <HoverCardContent className="absolute left-56 w-fit min-w-72 max-w-xl text-sm" side="right">
                        {workflowNodeDescription?.description && (
                            <div
                                className="flex"
                                dangerouslySetInnerHTML={{
                                    __html: sanitize(workflowNodeDescription.description, {
                                        allowedAttributes: {
                                            div: ['class'],
                                            table: ['class'],
                                            td: ['class'],
                                            tr: ['class'],
                                        },
                                    }),
                                }}
                            />
                        )}
                    </HoverCardContent>
                </HoverCardPortal>
            </HoverCard>

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="font-semibold">{data.title || data.label}</span>

                {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

                <span className="text-sm text-gray-500">{data.workflowNodeName}</span>
            </div>

            <Handle
                className={twMerge('left-node-handle-placement', styles.handle)}
                isConnectable={false}
                position={Position.Top}
                type="target"
            />

            <Handle
                className={twMerge('left-node-handle-placement', styles.handle)}
                isConnectable={false}
                position={Position.Bottom}
                type="source"
            />
        </div>
    );
};
export default memo(AiAgentNode);
