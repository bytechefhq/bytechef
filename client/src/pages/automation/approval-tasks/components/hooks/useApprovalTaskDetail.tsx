import {useUpdateApprovalTaskMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {ApprovalTaskI, AssigneeOptionI} from '../../types/types';
import {
    formatDate,
    getAssigneeNameById,
    getAvailableAssigneeOptions,
    getPriorityColor,
    getStatusIcon,
    toServerPriority,
    toServerStatus,
} from '../../utils/approval-task-utils';

import type {ReactNode} from 'react';

export interface UseApprovalTaskDetailReturnI {
    approvalTask: ApprovalTaskI | null;
    availableAssigneeOptions: AssigneeOptionI[];
    createdAtFormatted: string;
    handleApprovalSubmitted: () => void;
    handleAssigneeChange: (assigneeId: string) => void;
    handleDueDateChange: (dueDate: Date | undefined) => void;
    handlePriorityChange: (priority: ApprovalTaskI['priority']) => void;
    handleStatusChange: (status: ApprovalTaskI['status']) => void;
    priorityColor: string;
    statusIcon: ReactNode;
}

export function useApprovalTaskDetail(): UseApprovalTaskDetailReturnI {
    const {selectedApprovalTask, storeUpdateApprovalTask} = useApprovalTasksStore(
        useShallow((state) => ({
            selectedApprovalTask: state.getSelectedApprovalTask(),
            storeUpdateApprovalTask: state.updateApprovalTask,
        }))
    );

    const {data: usersData} = useUsersQuery();

    const availableAssigneeOptions = useMemo(() => getAvailableAssigneeOptions(usersData?.users?.content), [usersData]);

    const createdAtFormatted = useMemo(
        () => (selectedApprovalTask ? formatDate(selectedApprovalTask.createdAt) : ''),
        [selectedApprovalTask]
    );

    const priorityColor = useMemo(
        () => (selectedApprovalTask ? getPriorityColor(selectedApprovalTask.priority) : ''),
        [selectedApprovalTask]
    );

    const statusIcon = useMemo(
        () => (selectedApprovalTask ? getStatusIcon(selectedApprovalTask.status) : null),
        [selectedApprovalTask]
    );

    const queryClient = useQueryClient();

    const updateApprovalTaskMutation = useUpdateApprovalTaskMutation();

    const persistApprovalTask = useCallback(
        (next: ApprovalTaskI) => {
            const previous = selectedApprovalTask;

            storeUpdateApprovalTask(next);

            updateApprovalTaskMutation.mutate(
                {
                    approvalTask: {
                        assigneeId: next.assigneeId,
                        description: next.description,
                        dueDate: next.dueDate ?? null,
                        id: next.id,
                        name: next.title,
                        priority: toServerPriority(next.priority),
                        status: toServerStatus(next.status),
                        version: next.version,
                    },
                },
                {
                    onError: () => {
                        if (previous) {
                            storeUpdateApprovalTask(previous);
                        }

                        queryClient.invalidateQueries({queryKey: ['approvalTasks']});
                    },
                    onSuccess: (data) => {
                        storeUpdateApprovalTask({
                            ...next,
                            version: data.updateApprovalTask?.version ?? next.version,
                        });
                    },
                }
            );
        },
        [queryClient, selectedApprovalTask, storeUpdateApprovalTask, updateApprovalTaskMutation]
    );

    const handleStatusChange = useCallback(
        (status: ApprovalTaskI['status']) => {
            if (selectedApprovalTask) {
                persistApprovalTask({...selectedApprovalTask, status});
            }
        },
        [selectedApprovalTask, persistApprovalTask]
    );

    const handlePriorityChange = useCallback(
        (priority: ApprovalTaskI['priority']) => {
            if (selectedApprovalTask) {
                persistApprovalTask({...selectedApprovalTask, priority});
            }
        },
        [selectedApprovalTask, persistApprovalTask]
    );

    const handleAssigneeChange = useCallback(
        (assigneeId: string) => {
            if (selectedApprovalTask) {
                const assigneeName = getAssigneeNameById(assigneeId, usersData?.users?.content);

                persistApprovalTask({
                    ...selectedApprovalTask,
                    assignee: assigneeName,
                    assigneeId,
                });
            }
        },
        [selectedApprovalTask, usersData, persistApprovalTask]
    );

    const handleDueDateChange = useCallback(
        (dueDate: Date | undefined) => {
            if (selectedApprovalTask) {
                persistApprovalTask({
                    ...selectedApprovalTask,
                    dueDate: dueDate ? dueDate.toISOString() : undefined,
                });
            }
        },
        [selectedApprovalTask, persistApprovalTask]
    );

    const handleApprovalSubmitted = useCallback(() => {
        void queryClient.invalidateQueries({queryKey: ['approvalTasks']});
    }, [queryClient]);

    return {
        approvalTask: selectedApprovalTask,
        availableAssigneeOptions,
        createdAtFormatted,
        handleApprovalSubmitted,
        handleAssigneeChange,
        handleDueDateChange,
        handlePriorityChange,
        handleStatusChange,
        priorityColor,
        statusIcon,
    };
}
