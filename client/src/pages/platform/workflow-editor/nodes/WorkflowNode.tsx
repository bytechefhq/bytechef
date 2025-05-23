import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {
    ClusterElementDefinitionApi,
    ClusterElementDefinitionBasic,
    GetRootComponentClusterElementDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {
    ClusterElementDefinitionKeys,
    useGetClusterElementDefinitionQuery,
} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {NodeDataType} from '@/shared/types';
import {HoverCard, HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {ArrowLeftRightIcon, ComponentIcon, TrashIcon} from 'lucide-react';
import {memo, useState} from 'react';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';

import {convertNameToCamelCase, convertNameToSnakeCase} from '../../ai-agent-editor/utils/clusterElementsUtils';
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
    const {integrationId, projectId, workflow} = useWorkflowDataStore();
    const {clusterElementsCanvasOpen, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore();

    const handleNodeClick = useNodeClickHandler(data, id);

    const isSelected = currentNode?.name === data.name;

    const isRootClusterElement = data.rootClusterElement;

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            id: workflow.id!,
            workflowNodeName: hoveredNodeName!,
        },
        hoveredNodeName !== undefined && !data.clusterElementType
    );

    const {data: clusterElementDefinitionData} = useGetClusterElementDefinitionQuery(
        {
            componentName: data.componentName,
            componentVersion: data.version as number,
            // eslint-disable-next-line sort-keys
            clusterElementName: data.clusterElementName as string,
        },
        hoveredNodeName !== undefined && !!data.clusterElementType
    );

    const componentVersion = rootClusterElementNodeData?.type
        ? parseInt(rootClusterElementNodeData.type.split('/')[1].replace(/^v/, ''))
        : 1;

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementNodeData?.componentName || '',
            componentVersion: componentVersion || 1,
        },
        !!isRootClusterElement && clusterElementsCanvasOpen
    );

    const handleCount = rootClusterElementDefinition?.clusterElementTypes?.length;

    const getHandlePosition = (index: number, handleCount: number = 1): string => {
        const handleCountRange = Math.max(1, handleCount);

        const sectionCount = handleCountRange + 1;

        const sectionWidth = 120 / sectionCount;

        const handlePosition = Math.round(sectionWidth * (index + 1));

        return `${handlePosition}px`;
    };

    const queryClient = useQueryClient();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const isClusterElement = 'clusterElementType' in data;

    const handleDeleteNodeClick = (data: NodeDataType) => {
        if (data) {
            handleDeleteTask({
                clusterElementsCanvasOpen,
                currentNode,
                data,
                integrationId,
                projectId,
                queryClient,
                rootClusterElementNodeData,
                setCurrentNode,
                setRootClusterElementNodeData,
                updateWorkflowMutation,
                workflow,
            });
        }
    };

    const rootClusterElementComponentVersion = rootClusterElementNodeData?.type
        ? parseInt(rootClusterElementNodeData?.type?.split('/')[1].replace(/^v/, ''))
        : 1;

    const handlePopoverMenuClusterElementClick = (type: string) => {
        const rootComponentClusterElementDefinitionRequest: GetRootComponentClusterElementDefinitionsRequest = {
            clusterElementType: type,
            rootComponentName: rootClusterElementNodeData?.componentName || '',
            rootComponentVersion: rootClusterElementComponentVersion || 1,
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

    const nodeDescription =
        workflowNodeDescription?.description && !data.clusterElementType
            ? workflowNodeDescription.description
            : clusterElementDefinitionData?.description;

    return (
        <div
            className={twMerge(
                'relative flex min-w-60 cursor-pointer justify-center',
                !data.taskDispatcher && 'items-center',
                !isClusterElement && 'nodrag'
            )}
            data-nodetype={data.trigger ? 'trigger' : 'task'}
            key={id}
            onMouseOut={() => setIsHovered(false)}
            onMouseOver={() => setIsHovered(true)}
        >
            {isHovered && !isRootClusterElement && !isClusterElement && (
                <div className="absolute left-workflow-node-popover-hover pr-4">
                    {data.trigger ? (
                        <WorkflowNodesPopoverMenu
                            hideActionComponents
                            hideClusterElementComponents
                            hideTaskDispatchers
                            sourceNodeId={id}
                        >
                            <Button
                                className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                                title="Change trigger component"
                                variant="outline"
                            >
                                <ArrowLeftRightIcon className="size-4" />
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

            {isHovered && isClusterElement && (
                <div className="absolute left-workflow-node-popover-hover flex flex-col gap-1 pr-4">
                    {!data.multipleClusterElementsNode && currentNode && (
                        <WorkflowNodesPopoverMenu
                            clusterElementType={data.clusterElementType}
                            hideActionComponents={!!data.clusterElementType}
                            hideClusterElementComponents={!data.clusterElementType}
                            hideTaskDispatchers={!!data.clusterElementType}
                            hideTriggerComponents
                            sourceData={clusterElementDefinition}
                            sourceNodeId={currentNode.name}
                        >
                            <Button
                                className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                                onClick={() => {
                                    if (data.clusterElementType) {
                                        handlePopoverMenuClusterElementClick(
                                            convertNameToSnakeCase(data.clusterElementType)
                                        );
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
                                !isRootClusterElement &&
                                'border-blue-300 bg-blue-100 shadow-none',
                            isRootClusterElement && 'px-10'
                        )}
                        onClick={handleNodeClick}
                    >
                        {data.icon ? data.icon : <ComponentIcon className="size-9 text-black" />}
                    </Button>
                </HoverCardTrigger>

                <HoverCardPortal>
                    <HoverCardContent className="w-fit min-w-72 max-w-xl text-sm" side="right">
                        {nodeDescription && (
                            <div
                                className="flex"
                                dangerouslySetInnerHTML={{
                                    __html: sanitize(nodeDescription, {
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

            {!isRootClusterElement ? (
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
            ) : (
                <>
                    {rootClusterElementDefinition?.clusterElementTypes?.map((clusterElementType, index) => (
                        <Handle
                            className={twMerge(styles.handle)}
                            id={`${convertNameToCamelCase(clusterElementType.name as string)}-handle`}
                            isConnectable={false}
                            key={`${convertNameToCamelCase(clusterElementType.name as string)}-handle`}
                            position={Position.Bottom}
                            style={{left: getHandlePosition(index, handleCount)}}
                            type="source"
                        />
                    ))}
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
