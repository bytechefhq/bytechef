import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {
    ClusterElementDefinitionApi,
    ClusterElementDefinitionBasic,
    GetRootComponentClusterElementDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {NodeDataType} from '@/shared/types';
import {HoverCard, HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {ArrowLeftRightIcon, ComponentIcon, PencilIcon, TrashIcon} from 'lucide-react';
import {memo, useState} from 'react';
import {useParams} from 'react-router-dom';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';

import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '../utils/handleDeleteTask';
import styles from './NodeTypes.module.css';

const WorkflowNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isHovered, setIsHovered] = useState(false);
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();
    const [clusterElementDefinition, setClusterElementDefinition] = useState<ClusterElementDefinitionBasic[]>([]);

    const {currentNode, setCurrentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {workflow} = useWorkflowDataStore();
    const {aiAgentNodeData, aiAgentOpen} = useWorkflowEditorStore();

    const handleNodeClick = useNodeClickHandler(data, id);

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

    const {projectId} = useParams();

    const isAiAgentClusterElement = 'clusterElementType' in data;
    const isAiAgentNode = data.componentName === 'aiAgent';

    const handleDeleteNodeClick = (data: NodeDataType) => {
        if (data) {
            handleDeleteTask({
                aiAgentOpen,
                currentNode,
                data,
                projectId: +projectId!,
                queryClient,
                setCurrentNode,
                updateWorkflowMutation,
                workflow,
            });
        }
    };

    const handlePopoverMenuClusterElementClick = (type: string) => {
        const rootComponentClusterElementDefinitionRequest: GetRootComponentClusterElementDefinitionsRequest = {
            clusterElementType: type,
            rootComponentName: currentNode?.componentName || '',
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

    const clusterElementsData = aiAgentNodeData?.clusterElements;

    return (
        <div
            className={twMerge(
                'relative flex min-w-60 cursor-pointer justify-center',
                !data.taskDispatcher && 'items-center',
                !isAiAgentClusterElement && 'nodrag'
            )}
            data-nodetype={data.trigger ? 'trigger' : 'task'}
            key={id}
            onMouseOut={() => setIsHovered(false)}
            onMouseOver={() => setIsHovered(true)}
        >
            {isHovered && !isAiAgentNode && !isAiAgentClusterElement && (
                <div className="absolute left-workflow-node-popover-hover pr-4">
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
                            onClick={() => handleDeleteNodeClick(data)}
                            title="Delete a node"
                            variant="outline"
                        >
                            <TrashIcon className="size-4" />
                        </Button>
                    )}
                </div>
            )}

            {isHovered && isAiAgentClusterElement && (
                <div className="absolute left-workflow-node-popover-hover flex flex-col gap-1 pr-4">
                    {data.clusterElementType !== 'tools' && currentNode && (
                        <WorkflowNodesPopoverMenu
                            clusterElementsData={clusterElementsData}
                            sourceData={clusterElementDefinition}
                            sourceNodeId={currentNode.name}
                        >
                            <Button
                                className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                                onClick={() => {
                                    if (data.clusterElementType) {
                                        if (data.clusterElementType !== 'chatMemory') {
                                            handlePopoverMenuClusterElementClick(data.clusterElementType.toUpperCase());
                                        } else if (data.clusterElementType === 'chatMemory') {
                                            handlePopoverMenuClusterElementClick('CHAT_MEMORY');
                                        }
                                    }
                                }}
                                title={`Change ${data.clusterElementType} component`}
                                variant="outline"
                            >
                                <ArrowLeftRightIcon className="size-4" />
                            </Button>
                        </WorkflowNodesPopoverMenu>
                    )}

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
                <HoverCardTrigger>
                    <Button
                        className={twMerge(
                            'size-18 rounded-md border-2 border-gray-300 bg-white p-4 shadow hover:border-blue-200 hover:bg-blue-200 hover:shadow-none [&_svg]:size-9',
                            isSelected &&
                                workflowNodeDetailsPanelOpen &&
                                !isAiAgentNode &&
                                'border-blue-300 bg-blue-100 shadow-none'
                        )}
                        onClick={handleNodeClick}
                    >
                        {data.icon ? data.icon : <ComponentIcon className="size-9 text-black" />}
                    </Button>
                </HoverCardTrigger>

                <HoverCardPortal>
                    <HoverCardContent className="w-fit min-w-72 max-w-xl text-sm" side="right">
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

            {!isAiAgentNode && (
                <>
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
                </>
            )}

            {isAiAgentNode && (
                <>
                    <Handle
                        className={twMerge('left-3', styles.handle)}
                        id="rag-handle"
                        isConnectable={false}
                        position={Position.Bottom}
                        type="source"
                    />
                    <Handle
                        className={twMerge('left-6', styles.handle)}
                        id="chatMemory-handle"
                        isConnectable={false}
                        position={Position.Bottom}
                        type="source"
                    />
                    <Handle
                        className={twMerge('left-11', styles.handle)}
                        id="model-handle"
                        isConnectable={false}
                        position={Position.Bottom}
                        type="source"
                    />
                    <Handle
                        className={twMerge('left-14', styles.handle)}
                        id="tools-handle"
                        isConnectable={false}
                        position={Position.Bottom}
                        type="source"
                    />
                </>
            )}

            {data.name.includes('condition') && (
                <div className="absolute bottom-0 left-0 font-bold text-muted-foreground">
                    <span className="absolute -bottom-10 -left-16">TRUE</span>

                    <span className="absolute -bottom-10 left-24">FALSE</span>
                </div>
            )}
        </div>
    );
};

export default memo(WorkflowNode);
