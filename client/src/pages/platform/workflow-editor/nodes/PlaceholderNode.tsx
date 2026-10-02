import '@/shared/styles/dropdownMenu.css';
import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger} from '@/components/ui/context-menu';
import {FINAL_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {ClipboardPlusIcon, PlusIcon} from 'lucide-react';
import {memo, useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useCanvasDropzone from '../hooks/useCanvasDropzone';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {getContextFromPlaceholderNode} from '../utils/getTaskDispatcherContext';
import pasteNode from '../utils/pasteNode';
import DropzoneHighlight from './DropzoneHighlight';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [menuReady, setMenuReady] = useState(false);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const {nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const {copiedNode, copiedWorkflowId} = useWorkflowEditorStore(
        useShallow((state) => ({
            copiedNode: state.copiedNode,
            copiedWorkflowId: state.copiedWorkflowId,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const {dropzoneHandlers, isDropzoneActive} = useCanvasDropzone('task');

    const nodeIndex = nodes.findIndex((node) => node.id === id);
    const isClusterElement = !!data.clusterElementType;
    const isFinalPlaceholder = id === FINAL_PLACEHOLDER_NODE_ID;
    const rootClusterElementId = id.split('-')[0];
    const effectiveDirection = isClusterElement ? 'TB' : layoutDirection;

    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);

    const canPaste = useMemo(
        () => !clusterElementsCanvasOpen && !!copiedNode && copiedWorkflowId === workflow.id,
        [clusterElementsCanvasOpen, copiedNode, copiedWorkflowId, workflow.id]
    );

    const copiedNodeLabel = copiedNode?.label || '';

    const displayLabel = useMemo(() => {
        if (!copiedNode) {
            return '';
        }

        return `${copiedNodeLabel} (${copiedNode.name})`;
    }, [copiedNode, copiedNodeLabel]);

    const handlePasteClick = useCallback(() => {
        if (!updateWorkflowMutation) {
            return;
        }

        const placeholderNode = nodes.find((node) => node.id === id);

        const taskDispatcherContext = placeholderNode ? getContextFromPlaceholderNode(placeholderNode) : undefined;

        pasteNode({nodeIndex, taskDispatcherContext, updateWorkflowMutation});
    }, [id, nodeIndex, nodes, updateWorkflowMutation]);

    const handleOpenChange = (open: boolean) => {
        if (open) {
            setMenuReady(false);
            setTimeout(() => setMenuReady(true), 200);
        } else {
            setMenuReady(false);
        }
    };

    return (
        <ContextMenu onOpenChange={handleOpenChange}>
            <ContextMenuTrigger asChild disabled={!canPaste || isClusterElement}>
                <div>
                    <WorkflowNodesPopoverMenu
                        clusterElementType={data.clusterElementType}
                        hideActionComponents={!!data.clusterElementType}
                        hideClusterElementComponents={!data.clusterElementType}
                        hideTaskDispatchers={!!data.clusterElementType}
                        hideTriggerComponents
                        key={`${id}-${nodeIndex}`}
                        multipleClusterElementsNode={data.multipleClusterElementsNode}
                        nodeIndex={nodeIndex}
                        showPaste={canPaste}
                        sourceNodeId={data.clusterElementType ? rootClusterElementId : id}
                    >
                        <div
                            className={twMerge(
                                'nodrag relative mx-[22px] flex size-7 cursor-pointer items-center justify-center rounded-md bg-gray-300 text-lg text-content-neutral-secondary shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                                isClusterElement && 'mx-0 size-6',
                                isFinalPlaceholder &&
                                    'mx-3 size-12 border-2 border-dashed border-stroke-neutral-tertiary bg-surface-neutral-primary hover:scale-105 hover:border-stroke-brand-secondary-hover hover:bg-surface-neutral-primary hover:text-content-neutral-primary'
                            )}
                            title="Click to add a node"
                            {...dropzoneHandlers}
                        >
                            {isFinalPlaceholder ? <PlusIcon className="size-6" /> : data.label}

                            {isDropzoneActive && <DropzoneHighlight />}

                            <Handle
                                className={styles.handle}
                                position={mapHandlePosition(Position.Top, effectiveDirection)}
                                type="target"
                            />

                            <Handle
                                className={styles.handle}
                                position={mapHandlePosition(Position.Bottom, effectiveDirection)}
                                type="source"
                            />
                        </div>
                    </WorkflowNodesPopoverMenu>
                </div>
            </ContextMenuTrigger>

            <ContextMenuContent
                className={twMerge('w-workflow-node-context-menu-width p-0', !menuReady && 'pointer-events-none')}
            >
                <ContextMenuItem
                    className="dropdown-menu-item flex w-full flex-col items-start gap-1"
                    disabled={!canPaste}
                    onClick={handlePasteClick}
                >
                    <div className="flex w-full items-center gap-2 self-stretch text-content-neutral-primary">
                        <ClipboardPlusIcon className="size-4 shrink-0" />

                        <span>Paste Here</span>
                    </div>

                    <div className="flex w-full items-center gap-2 text-content-neutral-secondary">
                        <span className="flex size-4 shrink-0 items-center justify-center overflow-hidden [&>svg]:size-4">
                            {copiedNode?.icon ?? null}
                        </span>

                        <span className="line-clamp-1 flex-1 text-xs font-normal" title={displayLabel}>
                            {displayLabel}
                        </span>
                    </div>
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    );
};

export default memo(PlaceholderNode);
