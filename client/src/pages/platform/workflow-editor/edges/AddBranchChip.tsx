import Button from '@/components/Button/Button';
import {LayoutDirectionType} from '@/shared/constants';
import {EdgeLabelRenderer} from '@xyflow/react';
import {PlusIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import computeAddBranchChipPosition from './computeAddBranchChipPosition';

interface AddBranchChipProps {
    edgeId: string;
    layoutDirection: LayoutDirectionType;
    placeholderId: string;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

export default function AddBranchChip({
    edgeId,
    layoutDirection,
    placeholderId,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: AddBranchChipProps) {
    const {placeholderNodeIndex, workflowId} = useWorkflowDataStore(
        useShallow((state) => ({
            placeholderNodeIndex: state.nodes.findIndex((node) => node.id === placeholderId),
            workflowId: state.workflow.id,
        }))
    );

    const {copiedNode, copiedWorkflowId} = useWorkflowEditorStore(
        useShallow((state) => ({
            copiedNode: state.copiedNode,
            copiedWorkflowId: state.copiedWorkflowId,
        }))
    );

    const canPaste = !!copiedNode && !copiedNode.trigger && copiedWorkflowId === workflowId;

    const chipPosition = useMemo(
        () => computeAddBranchChipPosition({layoutDirection, sourceX, sourceY, targetX, targetY}),
        [layoutDirection, sourceX, sourceY, targetX, targetY]
    );

    if (placeholderNodeIndex === -1) {
        return null;
    }

    return (
        <EdgeLabelRenderer key={`${edgeId}-add-branch`}>
            <div
                className="nodrag nopan z-10 flex items-center rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-1 shadow-xs hover:border-stroke-brand-secondary-hover"
                style={{
                    pointerEvents: 'all',
                    position: 'absolute',
                    transform: `translate(-50%, -50%) translate(${chipPosition.x}px, ${chipPosition.y}px)`,
                }}
            >
                <WorkflowNodesPopoverMenu
                    hideClusterElementComponents
                    hideTriggerComponents
                    nodeIndex={placeholderNodeIndex}
                    showPaste={canPaste}
                    sourceNodeId={placeholderId}
                >
                    <Button
                        aria-label="Add branch"
                        className="size-auto cursor-pointer p-1 text-content-neutral-primary/50 hover:bg-surface-neutral-primary-hover hover:text-content-neutral-primary [&_svg]:size-4"
                        icon={<PlusIcon />}
                        size="icon"
                        title="Add branch"
                        variant="ghost"
                    />
                </WorkflowNodesPopoverMenu>
            </div>
        </EdgeLabelRenderer>
    );
}
