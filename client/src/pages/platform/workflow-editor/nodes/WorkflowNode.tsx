import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {NODE_WIDTH, ROOT_CLUSTER_WIDTH} from '@/shared/constants';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {NodeDataType} from '@/shared/types';
import {HoverCard, HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {ArrowLeftRightIcon, ComponentIcon, TrashIcon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {
    calculateNodeWidth,
    convertNameToCamelCase,
    getClusterElementTypesCount,
    getHandlePosition,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTask from '../utils/handleDeleteTask';
import saveClusterElementNodesPosition from '../utils/saveClusterElementNodesPosition';
import styles from './NodeTypes.module.css';

const WorkflowNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();

    const {currentNode, setCurrentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            setCurrentNode: state.setCurrentNode,
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );

    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const {clusterElementsCanvasOpen, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
                rootClusterElementNodeData: state.rootClusterElementNodeData,
                setRootClusterElementNodeData: state.setRootClusterElementNodeData,
            }))
        );

    const {invalidateWorkflowQueries} = useWorkflowEditor();

    const nodeClickHandler = useNodeClickHandler(data, id);

    const handleNodeClick = () => {
        if (clusterElementsCanvasOpen) {
            saveClusterElementNodesPosition({
                invalidateWorkflowQueries,
                updateWorkflowMutation,
                workflow,
            });
        }

        nodeClickHandler();
    };

    const isSelected = currentNode?.name === data.name;

    const isMainRootClusterElement = !!data.clusterRoot && !data.isNestedClusterRoot;
    const isClusterElement = !!data.clusterElementType;
    const isNestedClusterRoot = !!data.isNestedClusterRoot;
    const parentClusterRootId = data.parentClusterRootId || id.split('-')[0];

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

    const componentDefinitionKey = useMemo(() => {
        return {
            componentName: data.componentName,
            componentVersion: (data.version as number) || rootClusterElementComponentVersion,
        };
    }, [data.componentName, data.version, rootClusterElementComponentVersion]);

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        componentDefinitionKey,
        clusterElementsCanvasOpen && (!!isMainRootClusterElement || isNestedClusterRoot)
    );

    const clusterElementTypesCount = useMemo(() => {
        const clusterRootRequirementMet =
            clusterElementsCanvasOpen &&
            (isMainRootClusterElement || isNestedClusterRoot) &&
            rootClusterElementDefinition;

        if (!clusterRootRequirementMet) {
            return 0;
        }

        return getClusterElementTypesCount({
            clusterRootComponentDefinition: rootClusterElementDefinition,
            operationName: data.operationName,
        });
    }, [
        clusterElementsCanvasOpen,
        isNestedClusterRoot,
        isMainRootClusterElement,
        rootClusterElementDefinition,
        data.operationName,
    ]);

    const nodeWidth = useMemo(
        () =>
            clusterElementsCanvasOpen && (isMainRootClusterElement || isNestedClusterRoot)
                ? calculateNodeWidth(clusterElementTypesCount)
                : NODE_WIDTH,
        [clusterElementsCanvasOpen, isMainRootClusterElement, isNestedClusterRoot, clusterElementTypesCount]
    );

    const queryClient = useQueryClient();

    const {updateWorkflowMutation} = useWorkflowEditor();

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
                'group relative flex min-w-60 cursor-pointer justify-center',
                !data.taskDispatcher && 'items-center',
                !isClusterElement && !isMainRootClusterElement && 'nodrag',
                isClusterElement && !isNestedClusterRoot && 'w-[72px] min-w-[72px] flex-col items-center gap-1'
            )}
            data-nodetype={data.trigger ? 'trigger' : 'task'}
            key={id}
        >
            {!isMainRootClusterElement && !isClusterElement && (
                <div className="invisible absolute left-workflow-node-popover-hover pr-4 group-hover:visible">
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

            {isClusterElement && (
                <div className="invisible absolute left-workflow-node-popover-hover top-0 flex flex-col gap-1 pr-4 group-hover:visible">
                    {!data.multipleClusterElementsNode && currentNode && (
                        <WorkflowNodesPopoverMenu
                            clusterElementType={data.clusterElementType}
                            hideActionComponents={!!data.clusterElementType}
                            hideClusterElementComponents={!data.clusterElementType}
                            hideTaskDispatchers={!!data.clusterElementType}
                            hideTriggerComponents
                            sourceNodeId={data.clusterElementType ? parentClusterRootId : id}
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
                                !isMainRootClusterElement &&
                                'border-blue-300 bg-blue-100 shadow-none',
                            isMainRootClusterElement && 'nodrag',
                            (isMainRootClusterElement || isNestedClusterRoot) && `min-w-[${ROOT_CLUSTER_WIDTH}px]`
                        )}
                        onClick={() => handleNodeClick()}
                        style={
                            isMainRootClusterElement || isNestedClusterRoot ? {minWidth: `${nodeWidth}px`} : undefined
                        }
                    >
                        <div
                            className={twMerge(
                                (isMainRootClusterElement || isNestedClusterRoot) && 'flex items-center gap-4'
                            )}
                        >
                            {data.icon ? data.icon : <ComponentIcon className="size-9 text-black" />}

                            {(isMainRootClusterElement || isNestedClusterRoot) && (
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

                {!isMainRootClusterElement && (
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

            {!(isMainRootClusterElement || isNestedClusterRoot) && (
                <div
                    className={twMerge(
                        'ml-2 flex w-full min-w-max flex-col items-start',
                        isClusterElement && !isNestedClusterRoot && 'absolute top-full ml-0 items-center text-center'
                    )}
                >
                    <span className={twMerge('font-semibold', isClusterElement && 'text-sm')}>
                        {data.title || data.label}
                    </span>

                    {data.operationName && (
                        <pre className={twMerge('text-sm', isClusterElement && 'text-xs')}>{data.operationName}</pre>
                    )}

                    {!isClusterElement && <span className="text-sm text-gray-500">{data.workflowNodeName}</span>}
                </div>
            )}

            {!isMainRootClusterElement ? (
                <>
                    {isNestedClusterRoot && rootClusterElementDefinition?.clusterElementTypes?.length ? (
                        <>
                            <Handle
                                className={twMerge(`left-${nodeWidth / 2}px`, styles.handle)}
                                isConnectable={false}
                                position={Position.Top}
                                type="target"
                            />

                            {rootClusterElementDefinition.clusterElementTypes.map((clusterElementType, index) => (
                                <Handle
                                    className={twMerge(styles.handle)}
                                    id={`${convertNameToCamelCase(clusterElementType.name as string)}-handle`}
                                    isConnectable={false}
                                    key={`${convertNameToCamelCase(clusterElementType.name as string)}-handle`}
                                    position={Position.Bottom}
                                    style={{
                                        left: `${getHandlePosition({
                                            handlesCount: clusterElementTypesCount,
                                            index,
                                            nodeWidth,
                                        })}px`,
                                        transform: 'translateX(-50%)',
                                    }}
                                    type="source"
                                />
                            ))}
                        </>
                    ) : (
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
                                left: `${getHandlePosition({
                                    handlesCount: clusterElementTypesCount,
                                    index,
                                    nodeWidth,
                                })}px`,
                                transform: 'translateX(-50%)',
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
