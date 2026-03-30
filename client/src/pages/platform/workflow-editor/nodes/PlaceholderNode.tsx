import {ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger} from '@/components/ui/context-menu';
import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {ClipboardPasteIcon} from 'lucide-react';
import {memo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {getContextFromPlaceholderNode} from '../utils/getTaskDispatcherContext';
import pasteNode from '../utils/pasteNode';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

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

    const nodeIndex = nodes.findIndex((node) => node.id === id);
    const isClusterElement = !!data.clusterElementType;
    const rootClusterElementId = id.split('-')[0];
    const effectiveDirection = isClusterElement ? 'TB' : layoutDirection;

    const canPaste = !!copiedNode && copiedWorkflowId === workflow.id;

    return (
        <ContextMenu>
            <ContextMenuTrigger asChild disabled={!canPaste}>
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
                        sourceNodeId={data.clusterElementType ? rootClusterElementId : id}
                    >
                        <div
                            className={twMerge(
                                'nodrag relative mx-[22px] flex cursor-pointer items-center justify-center rounded-md text-lg text-gray-500 shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                                isDropzoneActive
                                    ? 'absolute ml-2 size-16 scale-150 cursor-pointer bg-blue-100'
                                    : 'size-7 bg-gray-300',
                                isClusterElement && 'mx-0 size-6'
                            )}
                            onDragEnter={() => setDropzoneActive(true)}
                            onDragLeave={() => setDropzoneActive(false)}
                            onDragOver={(event) => event.preventDefault()}
                            onDrop={() => setDropzoneActive(false)}
                            title="Click to add a node"
                        >
                            {data.label}

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

            <ContextMenuContent>
                <ContextMenuItem
                    onClick={() => {
                        const placeholderNode = nodes.find((node) => node.id === id);

                        const taskDispatcherContext = placeholderNode
                            ? getContextFromPlaceholderNode(placeholderNode)
                            : undefined;

                        pasteNode({
                            nodeIndex,
                            taskDispatcherContext,
                            updateWorkflowMutation: updateWorkflowMutation!,
                        });
                    }}
                >
                    <ClipboardPasteIcon />
                    Paste Here
                </ContextMenuItem>
            </ContextMenuContent>
        </ContextMenu>
    );
};

export default memo(PlaceholderNode);
