import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';

import resetDisplayConditionsQueries from '../../utils/resetDisplayConditionsQueries';
import {DisplayConditionsQueryTargetType} from './resolveDisplayConditionsQueryTarget';

interface UseDisplayConditionsRefreshAfterOperationChangeProps {
    activeTab: string;
    displayConditionsDataUpdatedAt: number;
    displayConditionsErrorUpdatedAt: number;
    displayConditionsQueryTarget: DisplayConditionsQueryTargetType;
    nodeName: string | undefined;
    nodeType: string | undefined;
    operationChangeInProgress: boolean;
    workflowId: string;
}

/**
 * Display conditions are cached per workflow node name, not per operation, while every operation or version switch
 * saves the whole workflow and blanks currentNode.displayConditions. Each completed switch save changes the node's
 * type, so a type change on the same node resets the cached conditions — once per save, which also covers a switch
 * queued behind another one. The returned flag keeps the Properties tab on a single skeleton from the moment a switch
 * starts until conditions fetched after the last reset have arrived, even when that fetch only starts once the
 * Properties tab is opened.
 */
export default function useDisplayConditionsRefreshAfterOperationChange({
    activeTab,
    displayConditionsDataUpdatedAt,
    displayConditionsErrorUpdatedAt,
    displayConditionsQueryTarget,
    nodeName,
    nodeType,
    operationChangeInProgress,
    workflowId,
}: UseDisplayConditionsRefreshAfterOperationChangeProps): boolean {
    const [refreshing, setRefreshing] = useState(false);

    const previousNodeRef = useRef<{name?: string; type?: string}>({name: nodeName, type: nodeType});
    const resetAtRef = useRef<number | undefined>(undefined);

    const queryClient = useQueryClient();

    useEffect(() => {
        if (operationChangeInProgress) {
            setRefreshing(true);
        }
    }, [operationChangeInProgress]);

    useEffect(() => {
        const previousNode = previousNodeRef.current;

        previousNodeRef.current = {name: nodeName, type: nodeType};

        if (previousNode.name !== nodeName) {
            resetAtRef.current = undefined;

            return;
        }

        if (previousNode.type !== undefined && previousNode.type !== nodeType) {
            resetAtRef.current = Date.now();

            resetDisplayConditionsQueries(queryClient, workflowId);

            setRefreshing(true);
        }
    }, [nodeName, nodeType, queryClient, workflowId]);

    useEffect(() => {
        if (!refreshing || operationChangeInProgress) {
            return;
        }

        const resetAt = resetAtRef.current;

        if (resetAt === undefined) {
            setRefreshing(false);

            return;
        }

        const settledAfterReset = Math.max(displayConditionsDataUpdatedAt, displayConditionsErrorUpdatedAt) >= resetAt;
        const nothingToAwait = displayConditionsQueryTarget === 'none' && activeTab === 'properties';

        if (settledAfterReset || nothingToAwait) {
            resetAtRef.current = undefined;

            setRefreshing(false);
        }
    }, [
        activeTab,
        displayConditionsDataUpdatedAt,
        displayConditionsErrorUpdatedAt,
        displayConditionsQueryTarget,
        operationChangeInProgress,
        refreshing,
    ]);

    return operationChangeInProgress || refreshing;
}
