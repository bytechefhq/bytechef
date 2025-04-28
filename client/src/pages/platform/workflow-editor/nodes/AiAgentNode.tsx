import {Button} from '@/components/ui/button';
import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {
    ClusterElementDefinitionApi,
    ClusterElementDefinitionBasic,
    GetRootComponentClusterElementDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {ClusterElementItemType, ClusterElementsType, NodeDataType} from '@/shared/types';
import {HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {ComponentIcon, CpuIcon, DatabaseIcon, MemoryStick, PlusIcon, TrashIcon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useNodeClickHandler from '../hooks/useNodeClick';
import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '../utils/handleDeleteTask';
import styles from './NodeTypes.module.css';

const AiAgentNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [clusterElementDefinition, setClusterElementDefinition] = useState<ClusterElementDefinitionBasic[]>([]);
    const [isHovered, setIsHovered] = useState(false);
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const {workflow} = useWorkflowDataStore();

    const queryClient = useQueryClient();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const {projectId} = useParams();

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
                projectId: +projectId!,
                queryClient,
                updateWorkflowMutation,
                workflow,
            });
        }
    };

    const handlePopoverMenuClusterElementClick = (type: string) => {
        const rootComponentClusterElementDefinitionRequest: GetRootComponentClusterElementDefinitionsRequest = {
            clusterElementType: type,
            rootComponentName: data?.componentName || '',
            rootComponentVersion: data.version || 1,
        };

        const fetchRootComponentClusterElementDefinition = async () => {
            const rootComponentClusterElementDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ClusterElementDefinitionApi().getRootComponentClusterElementDefinitions(
                        rootComponentClusterElementDefinitionRequest
                    ),
                queryKey: ClusterElementDefinitionKeys.filteredClusterElementDefinitions(
                    rootComponentClusterElementDefinitionRequest
                ),
            });

            setClusterElementDefinition(rootComponentClusterElementDefinition);
        };

        fetchRootComponentClusterElementDefinition();
    };

    const clusterElementsData = useMemo(() => {
        if (
            !data.clusterElements ||
            typeof data.clusterElements !== 'object' ||
            Object.keys(data.clusterElements).length === 0
        ) {
            return {
                chatMemory: null,
                model: null,
                rag: null,
                tools: [],
            };
        }

        return data.clusterElements as ClusterElementsType;
    }, [data.clusterElements]);

    const getElementDisplayName = (element: ClusterElementItemType | null | undefined): string | undefined => {
        if (!element) {
            return undefined;
        }

        const elementName = element.name.split('_')[0];
        const elementLabel = element.label?.split('_')[0];

        return element.label ? elementLabel : elementName;
    };

    const {agentMemoryName, agentModelName, agentRagName, agentToolNames} = useMemo(
        () => ({
            agentMemoryName: getElementDisplayName(clusterElementsData?.chatMemory) || '',
            agentModelName: getElementDisplayName(clusterElementsData?.model) || '',
            agentRagName: getElementDisplayName(clusterElementsData?.rag) || '',
            agentToolNames: clusterElementsData?.tools?.map(getElementDisplayName).filter(Boolean) || [],
        }),
        [clusterElementsData]
    );

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

                        <WorkflowNodesPopoverMenu
                            clusterElementsData={clusterElementsData}
                            sourceData={clusterElementDefinition}
                            sourceNodeId={id}
                        >
                            <Button
                                className="[&>span]:line-clamp-0 w-1/2 border border-stroke-neutral-secondary bg-background px-3 py-2 text-content-neutral-primary shadow-none hover:bg-surface-neutral-primary-hover [&>span]:truncate [&>svg]:max-w-4"
                                onClick={(event) => {
                                    event.stopPropagation();
                                    handlePopoverMenuClusterElementClick('MODEL');
                                }}
                            >
                                {agentModelName ? (
                                    <>
                                        <ComponentIcon /> {agentModelName}
                                    </>
                                ) : (
                                    <>
                                        <CpuIcon /> Model
                                    </>
                                )}
                            </Button>
                        </WorkflowNodesPopoverMenu>
                    </div>

                    <div className="flex w-full justify-between gap-1">
                        <WorkflowNodesPopoverMenu
                            clusterElementsData={clusterElementsData}
                            sourceData={clusterElementDefinition}
                            sourceNodeId={id}
                        >
                            <Button
                                className="rounded-full px-2 font-medium hover:bg-surface-neutral-secondary-hover [&>svg]:max-w-4"
                                onClick={(event) => {
                                    event.stopPropagation();
                                    handlePopoverMenuClusterElementClick('RAG');
                                }}
                                variant="outline"
                            >
                                {agentRagName ? (
                                    <>
                                        <ComponentIcon /> {agentRagName}
                                    </>
                                ) : (
                                    <>
                                        <DatabaseIcon /> Retrieval
                                    </>
                                )}
                            </Button>
                        </WorkflowNodesPopoverMenu>

                        <WorkflowNodesPopoverMenu
                            clusterElementsData={clusterElementsData}
                            sourceData={clusterElementDefinition}
                            sourceNodeId={id}
                        >
                            <Button
                                className="rounded-full px-2 hover:bg-surface-neutral-secondary-hover [&>svg]:max-w-4"
                                onClick={(event) => {
                                    event.stopPropagation();
                                    handlePopoverMenuClusterElementClick('CHAT_MEMORY');
                                }}
                                variant="outline"
                            >
                                {agentMemoryName ? (
                                    <>
                                        <ComponentIcon /> {agentMemoryName}
                                    </>
                                ) : (
                                    <>
                                        <MemoryStick /> Memory
                                    </>
                                )}
                            </Button>
                        </WorkflowNodesPopoverMenu>
                    </div>

                    <div className="flex w-full items-center">
                        {agentToolNames &&
                            agentToolNames.map((tool, index) => (
                                <Button key={index} onClick={(event) => event.stopPropagation()} variant="ghost">
                                    {tool}
                                </Button>
                            ))}

                        <WorkflowNodesPopoverMenu
                            clusterElementsData={clusterElementsData}
                            sourceData={clusterElementDefinition}
                            sourceNodeId={id}
                        >
                            <Button
                                className={twMerge(
                                    'rounded-full bg-surface-neutral-secondary px-3 hover:bg-surface-neutral-secondary-hover [&>svg]:max-w-4',
                                    !!agentToolNames?.length && 'rounded-full p-3'
                                )}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    handlePopoverMenuClusterElementClick('TOOLS');
                                }}
                                variant="ghost"
                            >
                                <PlusIcon className="size-4" />

                                <span className={twMerge(agentToolNames?.length ? 'hidden' : 'inline-block')}>
                                    Tool
                                </span>
                            </Button>
                        </WorkflowNodesPopoverMenu>
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
                className={twMerge('left-[115px]', styles.handle)}
                isConnectable={false}
                position={Position.Top}
                type="target"
            />

            <Handle
                className={twMerge('left-[115px]', styles.handle)}
                isConnectable={false}
                position={Position.Bottom}
                type="source"
            />
        </div>
    );
};
export default memo(AiAgentNode);
