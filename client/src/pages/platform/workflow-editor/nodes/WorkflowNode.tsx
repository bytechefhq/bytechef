import {Button} from '@/components/ui/button';
import {HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {NODE_WIDTH, ROOT_CLUSTER_WIDTH} from '@/shared/constants';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {NodeDataType} from '@/shared/types';
import {HoverCard, HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {ArrowLeftRightIcon, ComponentIcon, PinOff, TrashIcon} from 'lucide-react';
import {memo, useMemo, useState} from 'react';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {
    calculateNodeWidth,
    convertNameToCamelCase,
    getFilteredClusterElementTypes,
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

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
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
    const {
        clusterElementsCanvasOpen,
        mainClusterRootComponentDefinition,
        nestedClusterRootsComponentDefinitions,
        rootClusterElementNodeData,
        setRootClusterElementNodeData,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            mainClusterRootComponentDefinition: state.mainClusterRootComponentDefinition,
            nestedClusterRootsComponentDefinitions: state.nestedClusterRootsComponentDefinitions,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setRootClusterElementNodeData: state.setRootClusterElementNodeData,
        }))
    );

    const {invalidateWorkflowQueries} = useWorkflowEditor();

    const handleNodeClick = useNodeClickHandler(data, id);

    const isSelected = currentNode?.name === data.name;

    const isMainRootClusterElement = !!data.clusterRoot && !data.isNestedClusterRoot;
    const isClusterElement = data.clusterElementType;
    const isNestedClusterRoot = data.isNestedClusterRoot;
    const parentClusterRootId = data.parentClusterRootId;
    const hasSavedClusterElementPosition = data.metadata?.ui?.nodePosition;

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            environmentId: currentEnvironmentId,
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

    const filteredClusterElementTypes = useMemo(() => {
        const clusterRootRequirementMet =
            clusterElementsCanvasOpen &&
            (isMainRootClusterElement || isNestedClusterRoot) &&
            mainClusterRootComponentDefinition;

        if (!clusterRootRequirementMet) {
            return [];
        }

        const nestedClusterRootDefinition = nestedClusterRootsComponentDefinitions[data.componentName];

        return getFilteredClusterElementTypes({
            clusterRootComponentDefinition: nestedClusterRootDefinition || mainClusterRootComponentDefinition,
            currentClusterElementsType: data.clusterElementType,
            isNestedClusterRoot,
            operationName: data.operationName,
        });
    }, [
        clusterElementsCanvasOpen,
        isMainRootClusterElement,
        isNestedClusterRoot,
        mainClusterRootComponentDefinition,
        nestedClusterRootsComponentDefinitions,
        data.componentName,
        data.clusterElementType,
        data.operationName,
    ]);

    const clusterElementTypesCount = useMemo(() => {
        return filteredClusterElementTypes.length;
    }, [filteredClusterElementTypes]);

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

    const handleRemoveSavedClusterElementPosition = (clickedNodeName: string) => {
        if (!rootClusterElementNodeData) {
            return;
        }

        saveClusterElementNodesPosition({
            clickedNodeName,
            invalidateWorkflowQueries,
            updateWorkflowMutation,
            workflow,
        });
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
                <div className="invisible absolute left-[-80px] z-50 grid grid-cols-2 gap-1 pr-8 group-hover:visible">
                    <Button
                        className={twMerge(
                            'col-start-1 row-span-2 self-center bg-white p-2 shadow-md hover:text-red-500 hover:shadow-sm',
                            data.multipleClusterElementsNode && !hasSavedClusterElementPosition && 'col-start-2'
                        )}
                        onClick={() => handleDeleteNodeClick(data)}
                        title="Delete a node"
                        variant="outline"
                    >
                        <TrashIcon className="size-4" />
                    </Button>

                    {!data.multipleClusterElementsNode && (
                        <WorkflowNodesPopoverMenu
                            clusterElementType={data.clusterElementType}
                            hideActionComponents={!!data.clusterElementType}
                            hideClusterElementComponents={!data.clusterElementType}
                            hideTaskDispatchers={!!data.clusterElementType}
                            hideTriggerComponents
                            sourceNodeId={data.clusterElementType && parentClusterRootId ? parentClusterRootId : id}
                            sourceNodeName={data.workflowNodeName}
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

                    {hasSavedClusterElementPosition && (
                        <Button
                            className="bg-white p-2 shadow-md hover:text-blue-500 hover:shadow-sm"
                            onClick={() => handleRemoveSavedClusterElementPosition(data.workflowNodeName)}
                            title="Remove saved node position"
                            variant="outline"
                        >
                            <PinOff className="size-4" />
                        </Button>
                    )}
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
                            'size-18 rounded-md border-2 border-gray-300 bg-white p-4 text-primary shadow hover:border-blue-200 hover:bg-blue-200 hover:shadow-none [&_svg]:size-9',
                            isSelected &&
                                workflowNodeDetailsPanelOpen &&
                                !isMainRootClusterElement &&
                                'border-blue-300 bg-blue-100 shadow-none',
                            isMainRootClusterElement && 'nodrag',
                            (isMainRootClusterElement || isNestedClusterRoot) && `min-w-[${ROOT_CLUSTER_WIDTH}px] `,
                            isClusterElement && !isMainRootClusterElement && 'rounded-full',
                            isClusterElement && !hasSavedClusterElementPosition && 'border-dashed'
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

                    <span className="text-sm text-gray-500">{data.workflowNodeName}</span>
                </div>
            )}

            {isNestedClusterRoot || isMainRootClusterElement ? (
                <>
                    {!isMainRootClusterElement && (
                        <Handle
                            className={twMerge(`left-${nodeWidth / 2}px`, styles.handle)}
                            isConnectable={false}
                            position={Position.Top}
                            type="target"
                        />
                    )}

                    {filteredClusterElementTypes.map((clusterElementType, index) => (
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
