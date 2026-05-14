import Button from '@/components/Button/Button';
import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowNodeContextMenu from '@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType} from '@/shared/types';
import {HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {CheckIcon, ComponentIcon, PinOffIcon, TrashIcon} from 'lucide-react';
import {ChangeEvent, FocusEvent, KeyboardEvent, memo, useCallback, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {extractClusterElementIcons} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useNodeClickHandler from '../hooks/useNodeClick';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {getNodeLabel} from '../utils/getNodeLabel';
import {getTask} from '../utils/getTask';
import handleDeleteTask from '../utils/handleDeleteTask';
import pasteNode from '../utils/pasteNode';
import removeWorkflowNodePosition from '../utils/removeWorkflowNodePosition';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import styles from './NodeTypes.module.css';

const AiAgentNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();
    const [renameValue, setRenameValue] = useState('');
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            setCurrentNode: state.setCurrentNode,
        }))
    );
    const {incrementLayoutResetCounter, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            incrementLayoutResetCounter: state.incrementLayoutResetCounter,
            workflow: state.workflow,
        }))
    );
    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
    const {copiedNode, copiedWorkflowId, renamingNodeName, setCopiedNode, setCopiedWorkflowId, setRenamingNodeName} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                copiedNode: state.copiedNode,
                copiedWorkflowId: state.copiedWorkflowId,
                renamingNodeName: state.renamingNodeName,
                setCopiedNode: state.setCopiedNode,
                setCopiedWorkflowId: state.setCopiedWorkflowId,
                setRenamingNodeName: state.setRenamingNodeName,
            }))
        );

    const queryClient = useQueryClient();
    const {cancelWorkflowQueries, invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const memoizedIconsList = useMemo(() => {
        if (!workflow.definition) {
            return {iconsToShow: [], remainingIcons: []};
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const mainClusterRootTask = data?.workflowNodeName
            ? getTask({
                  tasks: workflowDefinitionTasks,
                  workflowNodeName: data.workflowNodeName,
              })
            : undefined;

        if (!mainClusterRootTask?.clusterElements) {
            return {iconsToShow: [], remainingIcons: []};
        }

        const iconsList = extractClusterElementIcons(mainClusterRootTask.clusterElements);

        if (!Array.isArray(iconsList)) {
            return {iconsToShow: [], remainingIcons: []};
        }

        const uniqueIcons = iconsList.reduce((uniqueIconsList, iconItem) => {
            if (!uniqueIconsList.has(iconItem.icon)) {
                uniqueIconsList.set(iconItem.icon, iconItem);
            }

            return uniqueIconsList;
        }, new Map<string, {icon: string; label: string}>());

        const uniqueIconsList = Array.from(uniqueIcons.values());

        return {
            iconsToShow: uniqueIconsList.slice(0, 5),
            remainingIcons: uniqueIconsList.slice(5),
        };
    }, [data?.workflowNodeName, workflow.definition]);

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: hoveredNodeName!,
        },
        hoveredNodeName !== undefined
    );

    const handleNodeClick = useNodeClickHandler(data, id);

    const hasSavedNodePosition = data.metadata?.ui?.nodePosition;

    const {tasks: workflowTasks, triggers: workflowTriggers} = workflow;

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

    const handleDeleteNodeClick = useCallback(
        (nodeData: NodeDataType) => {
            if (nodeData) {
                handleDeleteTask({
                    cancelWorkflowQueries: cancelWorkflowQueries!,
                    currentNode,
                    data: nodeData,
                    invalidateWorkflowQueries: invalidateWorkflowQueries!,
                    queryClient,
                    updateWorkflowMutation: updateWorkflowMutation!,
                    workflow,
                });
            }
        },
        [cancelWorkflowQueries, currentNode, invalidateWorkflowQueries, queryClient, updateWorkflowMutation, workflow]
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

    const handleCopyNode = useCallback(() => {
        setTimeout(() => {
            setCopiedNode({...data, label: nodeLabel ?? data.label});
            setCopiedWorkflowId(workflow.id);
        }, 200);
    }, [data, nodeLabel, setCopiedNode, setCopiedWorkflowId, workflow.id]);

    const handleDelete = useCallback(() => handleDeleteNodeClick(data), [data, handleDeleteNodeClick]);

    const handlePasteNode = useCallback(() => {
        pasteNode({
            cancelWorkflowQueries: cancelWorkflowQueries!,
            sourceNodeName: data.name,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    }, [cancelWorkflowQueries, data.name, updateWorkflowMutation]);

    const handleResetPosition = useCallback(
        () => handleRemoveNodePosition(data.name),
        [data.name, handleRemoveNodePosition]
    );

    const handleSwitch = useCallback(() => {}, []);

    const handleStartRename = useCallback(() => {
        setRenameValue(nodeLabel ?? '');
        setRenamingNodeName(data.name);
    }, [data.name, nodeLabel, setRenamingNodeName]);

    const handleRenameSubmit = useCallback(
        (newLabel: string) => {
            const trimmed = newLabel.trim();

            if (!trimmed || trimmed === nodeLabel) {
                setRenamingNodeName(undefined);

                return;
            }

            if (!workflow.definition) {
                setRenamingNodeName(undefined);

                return;
            }

            const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;
            const mainClusterRootTask = getTask({
                tasks: workflowDefinitionTasks,
                workflowNodeName: data.workflowNodeName,
            });

            if (!mainClusterRootTask) {
                setRenamingNodeName(undefined);

                return;
            }

            saveWorkflowDefinition({
                decorative: true,
                nodeData: {
                    ...mainClusterRootTask,
                    componentName: data.componentName,
                    label: trimmed,
                    workflowNodeName: data.workflowNodeName,
                },
                updateWorkflowMutation: updateWorkflowMutation!,
            });

            if (currentNode?.workflowNodeName === data.workflowNodeName) {
                setCurrentNode({...currentNode, label: trimmed});
            }

            setRenamingNodeName(undefined);
        },
        [currentNode, data, nodeLabel, setCurrentNode, setRenamingNodeName, updateWorkflowMutation, workflow]
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

    const handleRenameInputBlur = useCallback(
        (event: FocusEvent<HTMLInputElement>) => handleRenameSubmit(event.target.value),
        [handleRenameSubmit]
    );

    const handleRenameInputChange = useCallback(
        (event: ChangeEvent<HTMLInputElement>) => setRenameValue(event.target.value),
        [setRenameValue]
    );

    const handleRenameConfirmClick = useCallback(
        () => handleRenameSubmit(renameValue),
        [handleRenameSubmit, renameValue]
    );

    const hasIcons = useMemo(() => memoizedIconsList.iconsToShow.length > 0, [memoizedIconsList.iconsToShow]);

    // Must mirror the layout's hasValidClusterElements check exactly (layoutUtils.tsx line 510-514)
    // to keep handle position in sync with the -85px cross-axis offset applied during layout.
    // Using data.clusterElements (from node props) instead of workflow.definition (from store)
    // prevents a timing mismatch when switching between workflows.
    const hasValidClusterElements = useMemo(
        () =>
            !!data.clusterElements &&
            Object.entries(data.clusterElements as Record<string, unknown>).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            ),
        [data.clusterElements]
    );

    const isRenaming = renamingNodeName === data.name;

    const canPaste = useMemo(
        () => !!copiedNode && copiedWorkflowId === workflow.id,
        [copiedNode, copiedWorkflowId, workflow.id]
    );

    const suppressHover = isRenaming;
    const isHorizontal = layoutDirection === 'LR';

    return (
        <WorkflowNodeContextMenu
            canPaste={canPaste}
            data={data}
            hasSavedPosition={!!hasSavedNodePosition}
            onCopy={handleCopyNode}
            onDelete={handleDelete}
            onPaste={handlePasteNode}
            onRename={handleStartRename}
            onResetPosition={handleResetPosition}
            onSwitch={handleSwitch}
            showCopyAction
            showDeleteAction
            showRenameAction
        >
            <div
                className={twMerge(
                    'group relative flex min-w-60 cursor-pointer items-center justify-center',
                    isHorizontal && !hasIcons && 'min-w-0'
                )}
                data-nodetype="clusterRoot"
                key={id}
            >
                <div
                    className={twMerge(
                        'invisible absolute left-workflow-node-popover-hover top-0 pr-4',
                        !suppressHover && 'group-hover:visible'
                    )}
                >
                    <div className="flex flex-col gap-1">
                        <Button
                            className="opacity-100"
                            icon={<TrashIcon />}
                            onClick={() => handleDeleteNodeClick(data)}
                            size="iconSm"
                            title="Delete a node"
                            variant="destructiveGhost"
                        />

                        {hasSavedNodePosition && (
                            <Button
                                icon={<PinOffIcon />}
                                onClick={() => handleRemoveNodePosition(data.name)}
                                size="iconSm"
                                title="Remove saved node position"
                                variant="ghost"
                            />
                        )}
                    </div>
                </div>

                <HoverCard
                    key={id}
                    onOpenChange={(open) => {
                        if (open) {
                            setHoveredNodeName(data.name);
                        } else {
                            setHoveredNodeName(undefined);
                        }
                    }}
                    open={false}
                >
                    <HoverCardTrigger>
                        <Button
                            className="size-18 flex w-full flex-col items-center justify-center rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-4 shadow hover:border-stroke-brand-secondary-hover hover:bg-surface-neutral-primary hover:shadow-none focus-visible:ring-stroke-brand-focus active:bg-surface-neutral-primary"
                            onClick={handleNodeClick}
                        >
                            <span className="self-center text-content-neutral-primary [&_svg]:size-9">
                                {data.icon ? (
                                    data.icon
                                ) : (
                                    <ComponentIcon className="size-9 text-content-neutral-primary" />
                                )}
                            </span>

                            {memoizedIconsList.iconsToShow.length > 0 && (
                                <ul
                                    className={twMerge(
                                        'mt-2 flex min-w-52 items-center justify-start',
                                        !hasIcons && 'hidden'
                                    )}
                                >
                                    {memoizedIconsList.iconsToShow.map((iconUrlObject, index) => (
                                        <Tooltip key={index}>
                                            <TooltipTrigger asChild>
                                                <li
                                                    className="mr-2 flex items-center justify-center rounded-full border bg-surface-neutral-primary p-1 [&_svg]:size-5"
                                                    key={index}
                                                >
                                                    {iconUrlObject ? (
                                                        <InlineSVG
                                                            className="size-9 flex-none text-content-neutral-primary"
                                                            src={iconUrlObject.icon}
                                                        />
                                                    ) : (
                                                        <Skeleton className="size-9 rounded-full" />
                                                    )}
                                                </li>
                                            </TooltipTrigger>

                                            <TooltipContent
                                                className="text-pretty border border-stroke-neutral-tertiary bg-surface-neutral-primary text-content-neutral-primary"
                                                side="bottom"
                                            >
                                                {iconUrlObject?.label}
                                            </TooltipContent>
                                        </Tooltip>
                                    ))}

                                    {memoizedIconsList.remainingIcons.length > 0 && (
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-surface-neutral-primary p-1">
                                                    <span className="self-center text-xs font-bold text-content-neutral-secondary">
                                                        +{memoizedIconsList.remainingIcons.length}
                                                    </span>
                                                </div>
                                            </TooltipTrigger>

                                            <TooltipContent
                                                className="max-w-36 text-pretty border border-stroke-neutral-tertiary bg-surface-neutral-primary text-content-neutral-primary"
                                                side="bottom"
                                            >
                                                <ul>
                                                    {memoizedIconsList.remainingIcons.map((iconUrlObject, index) => (
                                                        <li className="my-2" key={index}>
                                                            {iconUrlObject.label}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </TooltipContent>
                                        </Tooltip>
                                    )}
                                </ul>
                            )}
                        </Button>
                    </HoverCardTrigger>

                    {!clusterElementsCanvasOpen && (
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
                    )}
                </HoverCard>

                <div
                    className={twMerge(
                        'ml-2 flex w-full min-w-max flex-col items-start',
                        isHorizontal && 'absolute top-full ml-0 w-auto min-w-0 max-w-[150px] items-center text-center'
                    )}
                >
                    {isRenaming ? (
                        <div className="z-10 flex max-h-7 items-center rounded-md border-2 bg-surface-neutral-primary p-1">
                            <input
                                autoFocus
                                className="nodrag max-h-5 w-40 cursor-text select-text rounded border border-stroke-neutral-secondary bg-surface-neutral-secondary px-2 py-1 text-sm font-semibold outline-none hover:bg-surface-neutral-secondary-hover"
                                onBlur={handleRenameInputBlur}
                                onChange={handleRenameInputChange}
                                onKeyDown={handleRenameKeyDown}
                                onMouseDown={(event) => event.stopPropagation()}
                                value={renameValue}
                            />

                            <Button
                                className="ml-1 size-5 shrink-0 cursor-pointer [&_svg]:size-4"
                                icon={<CheckIcon className="text-content-brand-primary" />}
                                onClick={handleRenameConfirmClick}
                                size="icon"
                                variant="ghost"
                            />
                        </div>
                    ) : (
                        <span className={twMerge('font-semibold', isHorizontal && 'w-full truncate')}>{nodeLabel}</span>
                    )}

                    {data.operationName && (
                        <pre className={twMerge('text-sm', isHorizontal && 'w-full truncate')}>
                            {data.operationName}
                        </pre>
                    )}

                    <span
                        className={twMerge('text-sm text-content-neutral-secondary', isHorizontal && 'w-full truncate')}
                    >
                        {data.workflowNodeName}
                    </span>
                </div>

                <Handle
                    className={twMerge(
                        styles.handleVisible,
                        layoutDirection === 'LR'
                            ? '-left-[1px] rounded-l-xs rounded-r-none'
                            : '-top-[1px] rounded-b-none rounded-t-xs'
                    )}
                    isConnectable={false}
                    position={mapHandlePosition(Position.Top, layoutDirection)}
                    style={
                        layoutDirection === 'TB'
                            ? hasValidClusterElements
                                ? {left: '120px'}
                                : {left: '36px'}
                            : undefined
                    }
                    type="target"
                />

                <Handle
                    className={twMerge(
                        styles.handleVisible,
                        layoutDirection === 'LR' ? 'rounded-l-none rounded-r-xs' : 'rounded-b-xs rounded-t-none'
                    )}
                    isConnectable={false}
                    position={mapHandlePosition(Position.Bottom, layoutDirection)}
                    style={
                        layoutDirection === 'TB'
                            ? hasValidClusterElements
                                ? {left: '120px'}
                                : {left: '36px'}
                            : undefined
                    }
                    type="source"
                />
            </div>
        </WorkflowNodeContextMenu>
    );
};
export default memo(AiAgentNode);
