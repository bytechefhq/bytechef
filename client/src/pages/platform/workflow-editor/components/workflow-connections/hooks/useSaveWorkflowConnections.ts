import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import invalidateWorkflowValidation from '@/pages/platform/workflow-editor/utils/invalidateWorkflowValidation';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/middleware/graphql';
import {useDeleteWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';
import {toast} from 'sonner';
import {create} from 'zustand';

import {WorkflowConnectionChangeI} from '../utils/workflowConnectionGroups';

export interface SaveWorkflowConnectionsResultI {
    failedChange?: WorkflowConnectionChangeI;
    savedCount: number;
}

interface UseSaveWorkflowConnectionsProps {
    environmentId: number;
    workflowId: string;
}

interface SaveWorkflowConnectionsStoreI {
    pendingBatchCount: number;
}

/**
 * A cluster element's connection is stored under its cluster root, keyed by the element's node name, so the open
 * element panel matches on the key; any other node matches on its own node name.
 */
function changeTargetsNode(change: WorkflowConnectionChangeI, node: NodeDataType): boolean {
    if (node.clusterElementType) {
        return change.workflowConnectionKey === node.workflowNodeName;
    }

    return change.workflowNodeName === node.workflowNodeName;
}

/**
 * The tail of the one queue every batch joins. It lives at module scope rather than in a ref so a batch that is
 * still running when the sheet closes keeps blocking the batches of a reopened sheet.
 */
let queueTail: Promise<unknown> = Promise.resolve();

/**
 * How many batches are queued or running. It lives next to the queue for the same reason, and lets the sheet shell
 * refuse to close while a save is in flight.
 */
export const useSaveWorkflowConnectionsStore = create<SaveWorkflowConnectionsStoreI>()(() => ({
    pendingBatchCount: 0,
}));

function changePendingBatchCount(difference: number) {
    useSaveWorkflowConnectionsStore.setState((state) => ({pendingBatchCount: state.pendingBatchCount + difference}));
}

/**
 * Applies connection changes through the per-connection mutations, strictly one at a time. The workflow test
 * configuration is an optimistically locked aggregate, so every batch waits for the previous one to finish
 * (spec §4).
 */
export default function useSaveWorkflowConnections({environmentId, workflowId}: UseSaveWorkflowConnectionsProps) {
    const pending = useSaveWorkflowConnectionsStore((state) => state.pendingBatchCount > 0);

    const {ConnectionKeys} = useWorkflowEditor();

    const {mutateAsync: saveConnection} = useSaveWorkflowTestConfigurationConnectionMutation();
    const {mutateAsync: deleteConnection} = useDeleteWorkflowTestConfigurationConnectionMutation();

    const queryClient = useQueryClient();

    const applyChange = useCallback(
        async ({
            connectionId,
            previousConnectionId,
            workflowConnectionKey,
            workflowNodeName,
        }: WorkflowConnectionChangeI) => {
            if (connectionId === undefined) {
                await deleteConnection({
                    deleteWorkflowTestConfigurationConnectionRequest: {connectionId: previousConnectionId ?? 0},
                    environmentId,
                    workflowConnectionKey,
                    workflowId,
                    workflowNodeName,
                });

                return;
            }

            await saveConnection({connectionId, environmentId, workflowConnectionKey, workflowId, workflowNodeName});
        },
        [deleteConnection, environmentId, saveConnection, workflowId]
    );

    const refreshAfterBatch = useCallback(
        async (savedChanges: WorkflowConnectionChangeI[]) => {
            queryClient.removeQueries({
                queryKey: [...WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties, workflowId],
            });
            queryClient.removeQueries({queryKey: [...WorkflowNodeOptionKeys.workflowNodeOptions, workflowId]});
            queryClient.removeQueries({queryKey: [...WorkflowNodeOptionKeys.clusterElementNodeOptions, workflowId]});

            const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

            const currentNodeChange = currentNode
                ? savedChanges.findLast((change) => changeTargetsNode(change, currentNode))
                : undefined;

            if (currentNode && currentNodeChange) {
                setCurrentNode({...currentNode, connectionId: currentNodeChange.connectionId});
            }

            invalidateWorkflowValidation(queryClient);

            queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});

            await queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        },
        [ConnectionKeys, queryClient, workflowId]
    );

    const runBatch = useCallback(
        async (changes: WorkflowConnectionChangeI[]): Promise<SaveWorkflowConnectionsResultI> => {
            const savedChanges: WorkflowConnectionChangeI[] = [];

            let failedChange: WorkflowConnectionChangeI | undefined;

            for (const change of changes) {
                try {
                    await applyChange(change);
                } catch {
                    // The global fetch interceptor already shows the server error.
                    failedChange = change;

                    break;
                }

                savedChanges.push(change);
            }

            if (failedChange && savedChanges.length > 0) {
                const unchangedCount = changes.length - savedChanges.length;

                const unchangedLabel =
                    unchangedCount === 1 ? "1 task wasn't changed." : `${unchangedCount} tasks weren't changed.`;

                toast.error(`Updated ${savedChanges.length} of ${changes.length} tasks. ${unchangedLabel}`);
            }

            await refreshAfterBatch(savedChanges);

            return {failedChange, savedCount: savedChanges.length};
        },
        [applyChange, refreshAfterBatch]
    );

    const saveWorkflowConnections = useCallback(
        (changes: WorkflowConnectionChangeI[]): Promise<SaveWorkflowConnectionsResultI> => {
            if (changes.length === 0) {
                return Promise.resolve({savedCount: 0});
            }

            changePendingBatchCount(1);

            const batch = queueTail.then(() => runBatch(changes));

            queueTail = batch.catch(() => undefined);

            return batch.finally(() => changePendingBatchCount(-1));
        },
        [runBatch]
    );

    return {pending, saveWorkflowConnections};
}
