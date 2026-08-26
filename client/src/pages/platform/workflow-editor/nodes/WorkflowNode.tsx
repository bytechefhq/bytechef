import Button from '@/components/Button/Button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import WorkflowNodeContextMenu from '@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu';
import WorkflowNodeDropdownMenu from '@/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {getNodeLabel} from '@/pages/platform/workflow-editor/utils/getNodeLabel';
import {NODE_WIDTH} from '@/shared/constants';
import {useGetClusterElementDefinitionQuery} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ClusterElementsType, NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {CheckIcon, ComponentIcon, EllipsisVerticalIcon, Loader2Icon, TriangleAlertIcon, XIcon} from 'lucide-react';
import {KeyboardEvent, ReactNode, forwardRef, memo, useCallback, useMemo, useState} from 'react';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {
    calculateNodeWidth,
    convertNameToCamelCase,
    getFilteredClusterElementTypes,
    getHandlePosition,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useDisabledTaskNames from '../hooks/useDisabledTaskNames';
import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore, {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {getDisabledNodeReferences} from '../utils/getDisabledNodeReferences';
import {getTask} from '../utils/getTask';
import {getContextFromTaskNodeData} from '../utils/getTaskDispatcherContext';
import handleDeleteTask from '../utils/handleDeleteTask';
import handleDeleteTrigger from '../utils/handleDeleteTrigger';
import pasteNode from '../utils/pasteNode';
import removeWorkflowNodePosition from '../utils/removeWorkflowNodePosition';
import saveClusterElementNodesPosition from '../utils/saveClusterElementNodesPosition';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {toggleNodeDisabled} from '../utils/toggleNodeDisabled';
import DisabledNodeBadge from './DisabledNodeBadge';
import GraphTransitionHandles from './GraphTransitionHandles';
import styles from './NodeTypes.module.css';

type EffectiveDirectionType = Parameters<typeof mapHandlePosition>[1];
type NodePositionType = {x: number; y: number} | undefined;

function formatTestNodeDuration(durationMillis: number): string {
    if (durationMillis < 1000) {
        return `${durationMillis}ms`;
    }

    if (durationMillis < 60000) {
        return `${(durationMillis / 1000).toFixed(1)}s`;
    }

    const minutes = Math.floor(durationMillis / 60000);
    const seconds = Math.round((durationMillis % 60000) / 1000);

    return `${minutes}m ${seconds}s`;
}

/**
 * Advisory tooltip for a node whose parameters reference a disabled node. Deliberately names the
 * cause rather than a runtime outcome: a bare `${disabledName}` resolves to null, while
 * `${disabledName.field}` is left as the raw expression string (SpEL cannot read a property off
 * null and the evaluator returns the value unchanged), so "will resolve to null" would be wrong
 * for half the cases.
 */
function getDisabledReferenceWarning(referencedDisabledNames: Array<string>): string {
    if (referencedDisabledNames.length > 1) {
        return `References disabled nodes ${referencedDisabledNames.join(', ')} — they will not run, so this value will not resolve`;
    }

    return `References disabled node ${referencedDisabledNames[0]} — it will not run, so this value will not resolve`;
}

interface WorkflowNodeContentProps extends Omit<React.HTMLAttributes<HTMLDivElement>, 'id'> {
    clusterElementTypesCount: number;
    data: NodeDataType;
    effectiveDirection: EffectiveDirectionType;
    filteredClusterElementTypes: ReturnType<typeof getFilteredClusterElementTypes>;
    handleNodeClick: () => void;
    handleRenameKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void;
    handleRenameSubmit: (newLabel: string) => void;
    hasSavedClusterElementPosition: NodePositionType;
    id: string;
    infoCardOpen: boolean;
    isClusterElement: string | undefined;
    isEffectivelyDisabled: boolean;
    isHorizontal: boolean;
    isMainRootClusterElement: boolean;
    isNestedClusterRoot: boolean | undefined;
    isRegularNode: boolean;
    isRenaming: boolean;
    isSelected: boolean;
    nodeDescription: string | undefined;
    nodeLabel: string | undefined;
    nodeMenuOpen?: boolean;
    nodeMenuTrigger?: ReactNode;
    nodeWidth: number;
    onInfoClose: () => void;
    parentClusterRootId: string | undefined;
    referencedDisabledNames: Array<string>;
    renameValue: string;
    setRenameValue: (value: string) => void;
    setSwitchPopoverOpen: (open: boolean) => void;
    switchPopoverOpen: boolean;
    testNodeState?: WorkflowTestNodeStateI;
    workflowNodeDetailsPanelOpen: boolean;
}

const WorkflowNodeContent = forwardRef<HTMLDivElement, WorkflowNodeContentProps>(
    (
        {
            clusterElementTypesCount,
            data,
            effectiveDirection,
            filteredClusterElementTypes,
            handleNodeClick,
            handleRenameKeyDown,
            handleRenameSubmit,
            hasSavedClusterElementPosition,
            id,
            infoCardOpen,
            isClusterElement,
            isEffectivelyDisabled,
            isHorizontal,
            isMainRootClusterElement,
            isNestedClusterRoot,
            isRegularNode,
            isRenaming,
            isSelected,
            nodeDescription,
            nodeLabel,
            nodeMenuOpen,
            nodeMenuTrigger,
            nodeWidth,
            onInfoClose,
            parentClusterRootId,
            referencedDisabledNames,
            renameValue,
            setRenameValue,
            setSwitchPopoverOpen,
            switchPopoverOpen,
            testNodeState,
            workflowNodeDetailsPanelOpen,
            ...rest
        },
        ref
    ) => (
        <div
            ref={ref}
            {...rest}
            className={twMerge(
                'group relative flex min-w-60 cursor-pointer justify-center',
                !data.taskDispatcher && 'items-center',
                (data.trigger || isMainRootClusterElement) && 'nodrag',
                isClusterElement && !isNestedClusterRoot && 'w-[72px] min-w-[72px] flex-col items-center gap-1',
                isHorizontal && isRegularNode && 'min-w-0',
                // A graph member is spaced from its neighbour by how far its label reaches, measured
                // off this element — so the 240px floor above would report a label's worth of width
                // for every member whatever its label actually says, and auto-arrange would hold them
                // that far apart. Free-form members are positioned individually, so nothing here needs
                // the uniform width the chain layout wants.
                data.graphData && isRegularNode && 'min-w-0',
                isEffectivelyDisabled && 'opacity-50 grayscale',
                rest.className
            )}
            data-nodetype={data.trigger ? 'trigger' : 'task'}
            key={id}
        >
            {nodeMenuTrigger && (
                <div
                    className={twMerge(
                        'nodrag invisible absolute top-0 -left-8 z-10 group-hover:visible data-[open=true]:visible',
                        isClusterElement && !isNestedClusterRoot && 'top-0 left-12'
                    )}
                    data-open={nodeMenuOpen}
                    onMouseDown={(event) => event.stopPropagation()}
                >
                    {nodeMenuTrigger}
                </div>
            )}

            {!isMainRootClusterElement && !isClusterElement && data.trigger && (
                <WorkflowNodesPopoverMenu
                    hideActionComponents
                    hideClusterElementComponents
                    hideTaskDispatchers
                    onOpenChange={setSwitchPopoverOpen}
                    open={switchPopoverOpen}
                    sourceNodeId={id}
                />
            )}

            {isClusterElement && !data.multipleClusterElementsNode && (
                <WorkflowNodesPopoverMenu
                    clusterElementType={data.clusterElementType}
                    hideActionComponents={!!data.clusterElementType}
                    hideClusterElementComponents={!data.clusterElementType}
                    hideTaskDispatchers={!!data.clusterElementType}
                    hideTriggerComponents
                    onOpenChange={setSwitchPopoverOpen}
                    open={switchPopoverOpen}
                    sourceNodeId={data.clusterElementType && parentClusterRootId ? parentClusterRootId : id}
                    sourceNodeName={data.workflowNodeName}
                />
            )}

            <Popover
                onOpenChange={(open) => {
                    if (!open) onInfoClose();
                }}
                open={infoCardOpen}
            >
                <PopoverTrigger asChild>
                    <Button
                        aria-label={`${data.workflowNodeName} node`}
                        className={twMerge(
                            'h-auto min-h-18 rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-4 text-primary hover:border-stroke-brand-secondary-hover hover:bg-surface-neutral-primary focus-visible:ring-stroke-brand-focus active:bg-surface-neutral-primary [&_svg]:size-9',
                            // Roots size via the inline width style; w-18 (!important) would otherwise clobber it.
                            !isMainRootClusterElement && !isNestedClusterRoot && 'w-18',
                            isMainRootClusterElement && 'nodrag',
                            isNestedClusterRoot && 'overflow-hidden rounded-2xl px-6',
                            isClusterElement && !isMainRootClusterElement && !isNestedClusterRoot && 'rounded-full',
                            isClusterElement &&
                                !isNestedClusterRoot &&
                                !hasSavedClusterElementPosition &&
                                'border-dashed',
                            testNodeState && 'relative',
                            testNodeState?.status === 'RUNNING' && 'border-blue-500 hover:border-blue-500',
                            testNodeState?.status === 'COMPLETED' && 'border-green-500 hover:border-green-500',
                            testNodeState?.status === 'FAILED' && 'border-red-500 hover:border-red-500',
                            // Last, so it wins the twMerge conflict against the test-run border above:
                            // a node that has been run keeps a status colour for the rest of the
                            // session, and while selected the selection is what the border has to
                            // say. The status is still on the badge in the corner, which nothing here
                            // touches.
                            isSelected &&
                                workflowNodeDetailsPanelOpen &&
                                'border-stroke-brand-primary shadow-none hover:border-stroke-brand-primary'
                        )}
                        onClick={handleNodeClick}
                        style={
                            isMainRootClusterElement
                                ? {minWidth: `${nodeWidth}px`}
                                : isNestedClusterRoot
                                  ? {width: `${nodeWidth}px`}
                                  : undefined
                        }
                    >
                        {testNodeState && (
                            <span
                                className={twMerge(
                                    'absolute -top-3 -right-3 z-10 flex size-6 items-center justify-center rounded-full border-2 bg-surface-neutral-primary [&_svg.lucide]:size-3.5',
                                    testNodeState.status === 'RUNNING' && 'border-blue-500 text-blue-500',
                                    testNodeState.status === 'COMPLETED' && 'border-green-500 text-green-500',
                                    testNodeState.status === 'FAILED' && 'border-red-500 text-red-500'
                                )}
                                title={testNodeState.error}
                            >
                                {testNodeState.status === 'RUNNING' && (
                                    <Loader2Icon className="size-3.5 animate-spin" />
                                )}

                                {testNodeState.status === 'COMPLETED' && <CheckIcon className="size-3.5" />}

                                {testNodeState.status === 'FAILED' && <XIcon className="size-3.5" />}
                            </span>
                        )}

                        {testNodeState?.durationMillis != null && testNodeState.status !== 'RUNNING' && (
                            <span className="absolute -bottom-2.5 left-1/2 z-10 -translate-x-1/2 rounded-full border border-stroke-neutral-tertiary bg-surface-neutral-primary px-1.5 text-xs leading-4 text-content-neutral-secondary">
                                {formatTestNodeDuration(testNodeState.durationMillis)}
                            </span>
                        )}

                        <div
                            className={twMerge(
                                (isMainRootClusterElement || isNestedClusterRoot) && 'flex items-center gap-4',
                                isNestedClusterRoot && 'min-w-0'
                            )}
                        >
                            {data.icon ? data.icon : <ComponentIcon className="size-9 text-content-neutral-primary" />}

                            {(isMainRootClusterElement || isNestedClusterRoot) && (
                                <div
                                    className={twMerge(
                                        'flex w-full flex-col items-start',
                                        isMainRootClusterElement && 'min-w-max',
                                        isNestedClusterRoot && 'min-w-0 overflow-hidden'
                                    )}
                                >
                                    {!(isNestedClusterRoot && isRenaming) && (
                                        <span
                                            className={twMerge(
                                                'font-semibold',
                                                isNestedClusterRoot && 'w-full truncate'
                                            )}
                                        >
                                            {nodeLabel}
                                        </span>
                                    )}

                                    {data.operationName && (
                                        <pre className={twMerge('text-sm', isNestedClusterRoot && 'w-full truncate')}>
                                            {data.operationName}
                                        </pre>
                                    )}

                                    {isNestedClusterRoot && (
                                        <span className="w-full truncate text-xs text-content-neutral-secondary">
                                            {data.workflowNodeName}
                                        </span>
                                    )}
                                </div>
                            )}
                        </div>
                    </Button>
                </PopoverTrigger>

                {!isMainRootClusterElement && (
                    <PopoverContent
                        className="w-fit max-w-xl min-w-72 text-sm"
                        onFocusOutside={(event) => event.preventDefault()}
                        onOpenAutoFocus={(event) => event.preventDefault()}
                        side="right"
                    >
                        <div className="mb-2 flex items-center justify-between gap-2">
                            <h3 className="text-lg font-semibold">{nodeLabel}</h3>

                            <Button
                                className="hover:bg-transparent active:bg-transparent"
                                icon={<XIcon />}
                                onClick={onInfoClose}
                                size="iconXs"
                                title="Close"
                                variant="ghost"
                            />
                        </div>

                        {nodeDescription ? (
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
                        ) : (
                            <p className="text-xs text-content-neutral-secondary">No description available.</p>
                        )}
                    </PopoverContent>
                )}
            </Popover>

            {isNestedClusterRoot && isRenaming && (
                <div className="absolute top-full left-0 z-10 mt-1 flex max-h-7 items-center rounded-md border-2 bg-surface-neutral-primary p-1">
                    <input
                        autoFocus
                        className="nodrag max-h-5 w-40 cursor-text rounded border border-stroke-neutral-secondary bg-surface-neutral-secondary px-2 py-1 text-sm font-semibold outline-hidden select-text hover:bg-surface-neutral-secondary-hover"
                        onBlur={(event) => handleRenameSubmit(event.target.value)}
                        onChange={(event) => setRenameValue(event.target.value)}
                        onKeyDown={handleRenameKeyDown}
                        onMouseDown={(event) => event.stopPropagation()}
                        value={renameValue}
                    />

                    <Button
                        className="ml-1 size-5 shrink-0 cursor-pointer [&_svg]:size-4"
                        icon={<CheckIcon className="text-content-brand-primary" />}
                        onClick={() => handleRenameSubmit(renameValue)}
                        size="icon"
                        variant="ghost"
                    />
                </div>
            )}

            {!(isMainRootClusterElement || isNestedClusterRoot) && (
                <div
                    className={twMerge(
                        'ml-2 flex w-full min-w-max flex-col items-start',
                        ((isClusterElement && !isNestedClusterRoot) || (isHorizontal && isRegularNode)) &&
                            'absolute top-full ml-0 items-center text-center',
                        isClusterElement && !isNestedClusterRoot && 'w-auto max-w-[130px] min-w-0',
                        isHorizontal && isRegularNode && 'w-auto max-w-[150px] min-w-0'
                    )}
                >
                    {isRenaming ? (
                        <div className="z-10 flex max-h-7 items-center rounded-md border-2 bg-surface-neutral-primary p-1">
                            <div className="flex items-center">
                                <input
                                    autoFocus
                                    className="nodrag max-h-5 w-40 cursor-text rounded border border-stroke-neutral-secondary bg-surface-neutral-secondary px-2 py-1 text-sm font-semibold outline-hidden select-text hover:bg-surface-neutral-secondary-hover"
                                    onBlur={(event) => handleRenameSubmit(event.target.value)}
                                    onChange={(event) => setRenameValue(event.target.value)}
                                    onKeyDown={handleRenameKeyDown}
                                    onMouseDown={(event) => event.stopPropagation()}
                                    value={renameValue}
                                />

                                <Button
                                    className="ml-1 size-5 shrink-0 cursor-pointer [&_svg]:size-4"
                                    icon={<CheckIcon className="text-content-brand-primary" />}
                                    onClick={() => handleRenameSubmit(renameValue)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </div>
                        </div>
                    ) : (
                        <div className="flex w-full items-center gap-1">
                            <span
                                className={twMerge(
                                    'max-w-48 truncate font-semibold',
                                    isClusterElement && 'w-full truncate text-sm',
                                    isHorizontal && isRegularNode && 'w-full truncate'
                                )}
                            >
                                {nodeLabel}
                            </span>

                            {data.disabled && <DisabledNodeBadge />}

                            {referencedDisabledNames.length > 0 && (
                                <span title={getDisabledReferenceWarning(referencedDisabledNames)}>
                                    <TriangleAlertIcon
                                        aria-hidden
                                        className="size-3.5 shrink-0 text-content-warning-primary"
                                    />
                                </span>
                            )}
                        </div>
                    )}

                    {data.operationName && (
                        <pre
                            className={twMerge(
                                'text-sm',
                                isClusterElement && 'text-xs',
                                (isClusterElement || (isHorizontal && isRegularNode)) && 'w-full truncate'
                            )}
                        >
                            {data.operationName}
                        </pre>
                    )}

                    <span
                        className={twMerge(
                            'text-sm text-content-neutral-secondary',
                            (isClusterElement || (isHorizontal && isRegularNode)) && 'w-full truncate'
                        )}
                    >
                        {data.workflowNodeName}
                    </span>
                </div>
            )}

            {isNestedClusterRoot || isMainRootClusterElement ? (
                <>
                    {!isMainRootClusterElement && (
                        <Handle
                            className={twMerge(`left-${nodeWidth / 2}px`, styles.handleVisible)}
                            isConnectable={false}
                            position={mapHandlePosition(Position.Top, effectiveDirection)}
                            type="target"
                        />
                    )}

                    {filteredClusterElementTypes.map((clusterElementType, index) => (
                        <Handle
                            className={styles.handle}
                            id={`${convertNameToCamelCase(clusterElementType.name as string)}-handle`}
                            isConnectable={false}
                            key={`${convertNameToCamelCase(clusterElementType.name as string)}-handle`}
                            position={mapHandlePosition(Position.Bottom, effectiveDirection)}
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
                        className={twMerge(
                            styles.handleVisible,
                            effectiveDirection === 'LR'
                                ? '-left-px rounded-l-xs rounded-r-none'
                                : 'rounded-t-xs rounded-b-none',
                            data.trigger && 'hidden'
                        )}
                        isConnectable={false}
                        position={mapHandlePosition(Position.Top, effectiveDirection)}
                        style={effectiveDirection === 'TB' ? {left: '36px'} : undefined}
                        type="target"
                    />

                    <Handle
                        className={twMerge(
                            styles.handleVisible,
                            effectiveDirection === 'LR' ? 'rounded-l-none rounded-r-xs' : 'rounded-t-none rounded-b-xs'
                        )}
                        isConnectable={false}
                        position={mapHandlePosition(Position.Bottom, effectiveDirection)}
                        style={effectiveDirection === 'TB' ? {left: '36px'} : undefined}
                        type="source"
                    />

                    {/* `WorkflowNode` never renders in a read-only workflow — `useLayout` converts
                        every task node to `readonly` there — so a member rendered by this
                        component is always on an editable canvas and its handles are connectable. */}
                    {data.graphData && (
                        <GraphTransitionHandles boxWidth={72} connectable direction={effectiveDirection} nodeId={id} />
                    )}
                </>
            )}

            {data.name.includes('condition') && effectiveDirection === 'TB' && (
                <div className="absolute bottom-0 left-0 font-bold text-muted-foreground">
                    <span className="absolute -bottom-7 -left-16">TRUE</span>

                    <span className="absolute -bottom-7 left-24">FALSE</span>
                </div>
            )}

            {data.name.includes('condition') && effectiveDirection === 'LR' && (
                <div className="absolute top-0 right-0 font-bold text-muted-foreground">
                    {/* equal widths + centered text keep both rotated labels on one vertical axis */}

                    <span className="absolute -top-8 -right-13 w-14 -rotate-90 text-center">TRUE</span>

                    <span className="absolute top-20 -right-13 w-14 -rotate-90 text-center">FALSE</span>
                </div>
            )}

            {data.componentName === 'on-error' && effectiveDirection === 'TB' && (
                <div className="absolute bottom-0 left-0 font-bold text-muted-foreground">
                    <span className="absolute -bottom-7 -left-16">TRY</span>

                    <span className="absolute -bottom-7 left-24">CATCH</span>
                </div>
            )}

            {data.componentName === 'on-error' && effectiveDirection === 'LR' && (
                <div className="absolute top-0 right-0 font-bold text-muted-foreground">
                    {/* equal widths + centered text keep both rotated labels on one vertical axis */}

                    <span className="absolute -top-8 -right-13 w-14 -rotate-90 text-center">TRY</span>

                    <span className="absolute top-20 -right-13 w-14 -rotate-90 text-center">CATCH</span>
                </div>
            )}
        </div>
    )
);

WorkflowNodeContent.displayName = 'WorkflowNodeContent';

const WorkflowNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [infoCardOpen, setInfoCardOpen] = useState(false);
    const [nodeMenuOpen, setNodeMenuOpen] = useState(false);
    const [renameValue, setRenameValue] = useState('');
    const [switchPopoverOpen, setSwitchPopoverOpen] = useState(false);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {currentNode, setCurrentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            setCurrentNode: state.setCurrentNode,
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );

    const {incrementLayoutResetCounter, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            incrementLayoutResetCounter: state.incrementLayoutResetCounter,
            workflow: state.workflow,
        }))
    );

    const {
        clusterElementsCanvasOpen,
        copiedNode,
        copiedWorkflowId,
        mainClusterRootComponentDefinition,
        nestedClusterRootsComponentDefinitions,
        renamingNodeName,
        rootClusterElementNodeData,
        setCopiedNode,
        setCopiedWorkflowId,
        setRenamingNodeName,
        setRootClusterElementNodeData,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            copiedNode: state.copiedNode,
            copiedWorkflowId: state.copiedWorkflowId,
            mainClusterRootComponentDefinition: state.mainClusterRootComponentDefinition,
            nestedClusterRootsComponentDefinitions: state.nestedClusterRootsComponentDefinitions,
            renamingNodeName: state.renamingNodeName,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setCopiedNode: state.setCopiedNode,
            setCopiedWorkflowId: state.setCopiedWorkflowId,
            setRenamingNodeName: state.setRenamingNodeName,
            setRootClusterElementNodeData: state.setRootClusterElementNodeData,
        }))
    );

    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const disabledTaskNames = useDisabledTaskNames();

    const {cancelWorkflowQueries, invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const handleNodeClick = useNodeClickHandler(data, id);

    const queryClient = useQueryClient();

    const isHorizontal = layoutDirection === 'LR';

    const isSelected = currentNode?.name === data.name;

    const isMainRootClusterElement = !!data.clusterRoot && !data.isNestedClusterRoot;
    const isClusterElement = data.clusterElementType;
    const isNestedClusterRoot = data.isNestedClusterRoot;
    const isRegularNode = !isClusterElement && !isMainRootClusterElement && !isNestedClusterRoot;
    const isClusterCanvasNode = !isRegularNode;
    const effectiveDirection: EffectiveDirectionType = isClusterCanvasNode ? 'TB' : layoutDirection;
    const parentClusterRootId = data.parentClusterRootId;
    const hasSavedClusterElementPosition = data.metadata?.ui?.nodePosition;
    // A graph member is excluded: inside a frame a position is the model rather than a pin
    // override, so there is nothing to reset it back to.
    const hasSavedNodePosition = isRegularNode && !data.trigger && !data.graphData && data.metadata?.ui?.nodePosition;

    const isEffectivelyDisabled = Boolean(data.disabled) || disabledTaskNames.has(data.workflowNodeName);

    const {tasks: workflowTasks, triggers: workflowTriggers} = workflow;

    const triggerCount = workflowTriggers?.length ?? 0;

    const nodeLabel = useMemo(
        () =>
            getNodeLabel({
                fallbackLabel: data.title || data.label,
                workflow,
                workflowNodeName: data.workflowNodeName,
            }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [data.label, data.title, data.workflowNodeName, workflowTasks, workflowTriggers]
    );

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: data.name,
        },
        infoCardOpen && !data.clusterElementType
    );

    const {data: clusterElementDefinitionData} = useGetClusterElementDefinitionQuery(
        {
            clusterElementName: data.clusterElementName as string,
            componentName: data.componentName,
            componentVersion: data.version as number,
        },
        infoCardOpen && !!data.clusterElementType
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
            currentClusterElementsType: (data.clusterElementName as string) || data.clusterElementType,
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
        data.clusterElementName,
        data.clusterElementType,
        data.operationName,
    ]);

    const clusterElementTypesCount = filteredClusterElementTypes.length;

    const nodeWidth = useMemo(
        () =>
            clusterElementsCanvasOpen && (isMainRootClusterElement || isNestedClusterRoot)
                ? calculateNodeWidth(clusterElementTypesCount)
                : NODE_WIDTH,
        [clusterElementsCanvasOpen, isMainRootClusterElement, isNestedClusterRoot, clusterElementTypesCount]
    );

    const referencedDisabledNames = useMemo(
        () => (isEffectivelyDisabled ? [] : getDisabledNodeReferences(data.parameters, disabledTaskNames)),
        [data.parameters, disabledTaskNames, isEffectivelyDisabled]
    );

    const handleDeleteNodeClick = useCallback(
        (nodeData: NodeDataType) => {
            if (!nodeData) {
                return;
            }

            if (nodeData.trigger) {
                handleDeleteTrigger({
                    cancelWorkflowQueries: cancelWorkflowQueries!,
                    invalidateWorkflowQueries: invalidateWorkflowQueries!,
                    triggerName: nodeData.name,
                    updateWorkflowMutation: updateWorkflowMutation!,
                    workflow,
                });

                return;
            }

            handleDeleteTask({
                cancelWorkflowQueries: cancelWorkflowQueries!,
                clusterElementsCanvasOpen,
                currentNode,
                data: nodeData,
                invalidateWorkflowQueries: invalidateWorkflowQueries!,
                queryClient,
                rootClusterElementNodeData,
                setCurrentNode,
                setRootClusterElementNodeData,
                updateWorkflowMutation: updateWorkflowMutation!,
                workflow,
            });
        },
        [
            cancelWorkflowQueries,
            clusterElementsCanvasOpen,
            currentNode,
            invalidateWorkflowQueries,
            queryClient,
            rootClusterElementNodeData,
            setCurrentNode,
            setRootClusterElementNodeData,
            updateWorkflowMutation,
            workflow,
        ]
    );

    const handleToggleDisabledClick = useCallback(() => {
        toggleNodeDisabled({updateWorkflowMutation: updateWorkflowMutation!, workflowNodeName: data.workflowNodeName});
    }, [data.workflowNodeName, updateWorkflowMutation]);

    const handleRemoveSavedClusterElementPosition = useCallback(
        (clickedNodeName: string) => {
            if (!rootClusterElementNodeData) {
                return;
            }

            saveClusterElementNodesPosition({
                clickedNodeName,
                updateWorkflowMutation,
                workflow,
            });
        },
        [rootClusterElementNodeData, updateWorkflowMutation, workflow]
    );

    const handleRemoveNodePosition = useCallback(
        (nodeName: string) => {
            removeWorkflowNodePosition({
                incrementLayoutResetCounter,
                invalidateWorkflowQueries: invalidateWorkflowQueries!,
                nodeName,
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        },
        [incrementLayoutResetCounter, invalidateWorkflowQueries, updateWorkflowMutation]
    );

    const handleRenameSubmit = useCallback(
        (newLabel: string) => {
            const trimmed = newLabel.trim();

            if (trimmed && trimmed !== nodeLabel) {
                if (isClusterElement && rootClusterElementNodeData && workflow.definition) {
                    const workflowDefinition = JSON.parse(workflow.definition);
                    const workflowDefinitionTasks = workflowDefinition.tasks ?? [];

                    const mainClusterRootTask = getTask({
                        tasks: workflowDefinitionTasks,
                        workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                    });

                    if (!mainClusterRootTask?.clusterElements) {
                        setRenamingNodeName(undefined);

                        return;
                    }

                    const updatedClusterElements = updateClusterElementLabel(
                        mainClusterRootTask.clusterElements as ClusterElementsType,
                        data.workflowNodeName,
                        trimmed
                    );

                    saveWorkflowDefinition({
                        decorative: true,
                        nodeData: {
                            ...mainClusterRootTask,
                            clusterElements: updatedClusterElements,
                            componentName: rootClusterElementNodeData.componentName,
                            workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                        } as NodeDataType,
                        updateWorkflowMutation: updateWorkflowMutation!,
                    });
                } else {
                    saveWorkflowDefinition({
                        decorative: true,
                        nodeData: {
                            ...data,
                            label: trimmed,
                            name: data.workflowNodeName,
                        },
                        updateWorkflowMutation: updateWorkflowMutation!,
                    });
                }

                if (currentNode?.workflowNodeName === data.workflowNodeName) {
                    setCurrentNode({...currentNode, label: trimmed});
                }
            }

            setRenamingNodeName(undefined);
        },
        [
            currentNode,
            data,
            isClusterElement,
            nodeLabel,
            rootClusterElementNodeData,
            setCurrentNode,
            setRenamingNodeName,
            updateWorkflowMutation,
            workflow.definition,
        ]
    );

    const handleRenameKeyDown = useCallback(
        (event: KeyboardEvent<HTMLInputElement>) => {
            if (event.key === 'Enter') {
                handleRenameSubmit(renameValue);
            }

            if (event.key === 'Escape') {
                setRenamingNodeName(undefined);
            }
        },
        [handleRenameSubmit, renameValue, setRenamingNodeName]
    );

    const handleStartRename = useCallback(() => {
        setRenameValue(nodeLabel ?? '');
        setRenamingNodeName(data.name);
    }, [data.name, nodeLabel, setRenameValue, setRenamingNodeName]);

    const saveNodeToClipboard = useCallback(() => {
        setCopiedNode({...data, label: nodeLabel ?? data.label});
        setCopiedWorkflowId(workflow.id);
    }, [data, nodeLabel, setCopiedNode, setCopiedWorkflowId, workflow.id]);

    const handleCopyNode = useCallback(() => {
        setTimeout(saveNodeToClipboard, 200);
    }, [saveNodeToClipboard]);

    const handleCutNode = useCallback(() => {
        setTimeout(saveNodeToClipboard, 200);
        handleDeleteNodeClick(data);
    }, [handleDeleteNodeClick, saveNodeToClipboard, data]);

    const handlePasteNode = useCallback(() => {
        const taskDispatcherContext = data.taskDispatcher ? undefined : getContextFromTaskNodeData(data, 1);

        pasteNode({
            cancelWorkflowQueries: cancelWorkflowQueries!,
            sourceNodeName: data.name,
            taskDispatcherContext,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }, [cancelWorkflowQueries, data, updateWorkflowMutation]);

    const handleDelete = useCallback(() => handleDeleteNodeClick(data), [data, handleDeleteNodeClick]);

    const handleResetPosition = useCallback(
        () => handleRemoveNodePosition(data.name),
        [data.name, handleRemoveNodePosition]
    );

    const handleSwitch = useCallback(() => setSwitchPopoverOpen(true), []);

    const isRenaming = renamingNodeName === data.name;

    const canPaste = !!copiedNode && copiedWorkflowId === workflow.id;

    const nodeDescription =
        workflowNodeDescription?.description && !data.clusterElementType
            ? workflowNodeDescription.description
            : clusterElementDefinitionData?.description;

    if (!updateWorkflowMutation || !invalidateWorkflowQueries || !cancelWorkflowQueries) {
        return null;
    }

    const kebabButton = (
        <Button
            aria-label={`${data.workflowNodeName} node actions`}
            className="nodrag size-6 rounded-md border border-stroke-neutral-tertiary bg-surface-neutral-primary p-1 shadow-sm [&_svg]:size-4"
            icon={<EllipsisVerticalIcon />}
            size="iconXs"
            title="Node actions"
            variant="ghost"
        />
    );

    const regularNodeMenuTrigger = (
        <WorkflowNodeDropdownMenu
            canPaste={canPaste}
            data={data}
            hasSavedPosition={!!hasSavedNodePosition}
            onCopy={handleCopyNode}
            onCut={handleCutNode}
            onDelete={handleDelete}
            onInfo={() => setInfoCardOpen(true)}
            onOpenChange={setNodeMenuOpen}
            onPaste={handlePasteNode}
            onRename={handleStartRename}
            onResetPosition={handleResetPosition}
            onSwitch={handleSwitch}
            onToggleDisabled={handleToggleDisabledClick}
            showCopyAction
            showCutAction
            showDeleteAction={!data.trigger || triggerCount > 1}
            showDisableAction={!data.trigger}
            showInfoAction
            showRenameAction
            trigger={kebabButton}
        />
    );

    const clusterElementMenuTrigger = (
        <WorkflowNodeDropdownMenu
            data={data}
            hasSavedPosition={!!hasSavedClusterElementPosition}
            onDelete={handleDelete}
            onInfo={() => setInfoCardOpen(true)}
            onOpenChange={setNodeMenuOpen}
            onRename={handleStartRename}
            onResetPosition={() => handleRemoveSavedClusterElementPosition(data.workflowNodeName)}
            onSwitch={handleSwitch}
            showDeleteAction
            showInfoAction
            showRenameAction
            showReplaceAction={!data.multipleClusterElementsNode}
            trigger={kebabButton}
        />
    );

    const sharedContentProps = {
        clusterElementTypesCount,
        data,
        effectiveDirection,
        filteredClusterElementTypes,
        handleNodeClick,
        handleRenameKeyDown,
        handleRenameSubmit,
        hasSavedClusterElementPosition,
        id,
        infoCardOpen,
        isClusterElement,
        isEffectivelyDisabled,
        isHorizontal,
        isMainRootClusterElement,
        isNestedClusterRoot,
        isRegularNode,
        isRenaming,
        isSelected,
        nodeDescription,
        nodeLabel,
        nodeMenuOpen,
        nodeWidth,
        onInfoClose: () => setInfoCardOpen(false),
        parentClusterRootId,
        referencedDisabledNames,
        renameValue,
        setRenameValue,
        setSwitchPopoverOpen,
        switchPopoverOpen,
        testNodeState: workflowTestNodeStates[data.workflowNodeName],
        workflowNodeDetailsPanelOpen,
    } satisfies WorkflowNodeContentProps;

    if (isRegularNode) {
        return (
            <WorkflowNodeContextMenu
                canPaste={canPaste}
                data={data}
                hasSavedPosition={!!hasSavedNodePosition}
                onCopy={handleCopyNode}
                onCut={handleCutNode}
                onDelete={handleDelete}
                onInfo={() => setInfoCardOpen(true)}
                onPaste={handlePasteNode}
                onRename={handleStartRename}
                onResetPosition={handleResetPosition}
                onSwitch={handleSwitch}
                onToggleDisabled={handleToggleDisabledClick}
                showCopyAction={!data.trigger}
                showCutAction={!data.trigger}
                showDeleteAction={!data.trigger || triggerCount > 1}
                showDisableAction={!data.trigger}
                showInfoAction
                showRenameAction
            >
                <WorkflowNodeContent {...sharedContentProps} nodeMenuTrigger={regularNodeMenuTrigger} />
            </WorkflowNodeContextMenu>
        );
    }

    if (isClusterElement && !isMainRootClusterElement) {
        return (
            <WorkflowNodeContextMenu
                data={data}
                hasSavedPosition={!!hasSavedClusterElementPosition}
                onDelete={handleDelete}
                onInfo={() => setInfoCardOpen(true)}
                onRename={handleStartRename}
                onResetPosition={() => handleRemoveSavedClusterElementPosition(data.workflowNodeName)}
                onSwitch={handleSwitch}
                showDeleteAction
                showInfoAction
                showRenameAction
                showReplaceAction={!data.multipleClusterElementsNode}
            >
                <WorkflowNodeContent {...sharedContentProps} nodeMenuTrigger={clusterElementMenuTrigger} />
            </WorkflowNodeContextMenu>
        );
    }

    return <WorkflowNodeContent {...sharedContentProps} />;
};

function updateClusterElementLabel(
    clusterElements: ClusterElementsType,
    targetName: string,
    newLabel: string
): ClusterElementsType {
    const updatedClusterElements: ClusterElementsType = {};

    for (const [key, value] of Object.entries(clusterElements)) {
        if (Array.isArray(value)) {
            updatedClusterElements[key] = value.map((element) => {
                const withUpdatedNested = element.clusterElements
                    ? {
                          ...element,
                          clusterElements: updateClusterElementLabel(element.clusterElements, targetName, newLabel),
                      }
                    : element;

                return element.name === targetName ? {...withUpdatedNested, label: newLabel} : withUpdatedNested;
            });
        } else if (value !== null && typeof value === 'object') {
            const withUpdatedNested = value.clusterElements
                ? {...value, clusterElements: updateClusterElementLabel(value.clusterElements, targetName, newLabel)}
                : value;

            updatedClusterElements[key] =
                value.name === targetName ? {...withUpdatedNested, label: newLabel} : withUpdatedNested;
        } else {
            updatedClusterElements[key] = value;
        }
    }

    return updatedClusterElements;
}

export default memo(WorkflowNode);
