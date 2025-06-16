import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
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

import {convertNameToCamelCase} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '../utils/handleDeleteTask';
import saveClusterElementNodesPosition from '../utils/saveClusterElementNodesPosition';
import styles from './NodeTypes.module.css';

const WorkflowNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isHovered, setIsHovered] = useState(false);
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();

    const {currentNode, setCurrentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {workflow} = useWorkflowDataStore();
    const {clusterElementsCanvasOpen, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore();

    const {invalidateWorkflowQueries} = useWorkflowEditor();

    const nodeClickHandler = useNodeClickHandler(data, id);

    const handleNodeClick = () => {
        if (clusterElementsCanvasOpen) {
            saveClusterElementNodesPosition({
                invalidateWorkflowQueries,
                queryClient,
                updateWorkflowMutation,
                workflow,
            });
        }

        nodeClickHandler();
    };

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
            clusterElementName: data.clusterElementName as string,
            componentName: data.componentName,
            componentVersion: data.version as number,
        },
        hoveredNodeName !== undefined && !!data.clusterElementType
    );

    const rootClusterElementComponentVersion =
        Number(rootClusterElementNodeData?.type?.split('/')[1].replace(/^v/, '')) || 1;

    const rootClusterElementComponentName = rootClusterElementNodeData?.componentName || '';

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementComponentName,
            componentVersion: rootClusterElementComponentVersion,
        },
        clusterElementsCanvasOpen && !!isRootClusterElement && !!rootClusterElementNodeData
    );

    const getHandlePosition = (index: number, totalHandles: number = 1): string => {
        const nodeWidth = 252;
        const nodeEdgeBuffer = nodeWidth * 0.1;

        const usableNodeWidth = nodeWidth - nodeEdgeBuffer * 2;

        if (totalHandles === 1) {
            return `${nodeWidth / 2}px`;
        }

        const stepWidth = totalHandles > 1 ? usableNodeWidth / (totalHandles - 1) : 0;

        const handlePosition = nodeEdgeBuffer + stepWidth * index;

        return `${handlePosition}px`;
    };

    const queryClient = useQueryClient();

    const {updateWorkflowMutation} = useWorkflowEditor();

    const isClusterElement = 'clusterElementType' in data;

    const handleDeleteNodeClick = (data: NodeDataType) => {
        if (data) {
            handleDeleteTask({
                clusterElementsCanvasOpen,
                currentNode,
                data,
                invalidateWorkflowQueries: invalidateWorkflowQueries!,
                queryClient,
                rootClusterElementNodeData,
                setCurrentNode,
                setRootClusterElementNodeData,
                updateWorkflowMutation: updateWorkflowMutation!,
                workflow,
            });
        }
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
                !isClusterElement && !isRootClusterElement && 'nodrag',
                isClusterElement && 'flex-col items-start gap-1'
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
                <div className="absolute left-workflow-node-popover-hover top-0 flex flex-col gap-1 pr-4">
                    {!data.multipleClusterElementsNode && currentNode && (
                        <WorkflowNodesPopoverMenu
                            clusterElementType={data.clusterElementType}
                            hideActionComponents={!!data.clusterElementType}
                            hideClusterElementComponents={!data.clusterElementType}
                            hideTaskDispatchers={!!data.clusterElementType}
                            hideTriggerComponents
                            sourceNodeId={currentNode.name}
                        >
                            <Button
                                className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
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
                            isRootClusterElement && `min-w-[252px]`
                        )}
                        onClick={handleNodeClick}
                    >
                        <div className={twMerge(isRootClusterElement && 'flex items-center gap-4')}>
                            {data.icon ? data.icon : <ComponentIcon className="size-9 text-black" />}

                            {isRootClusterElement && (
                                <div className="flex w-full min-w-max flex-col items-start">
                                    <span className="font-semibold text-black">{data.title || data.label}</span>

                                    {data.operationName && (
                                        <pre className="text-sm text-black">{data.operationName}</pre>
                                    )}

                                    <span className="text-sm text-gray-500">{data.workflowNodeName}</span>
                                </div>
                            )}
                        </div>
                    </Button>
                </HoverCardTrigger>

                {!isRootClusterElement && (
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
                )}
            </HoverCard>

            {!isRootClusterElement && (
                <div className={twMerge('ml-2 flex w-full min-w-max flex-col items-start', isClusterElement && 'ml-0')}>
                    <span className="font-semibold">{data.title || data.label}</span>

                    {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

                    <span className="text-sm text-gray-500">{data.workflowNodeName}</span>
                </div>
            )}

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
                            style={{
                                left: getHandlePosition(
                                    index,
                                    rootClusterElementDefinition.clusterElementTypes?.length
                                ),
                            }}
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
