import {
    ApprovalTask,
    useApprovalTasksQuery,
    useUpdateApprovalTaskMutation,
    useUsersQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useCallback, useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/shallow';

import {useApprovalTasksStore} from '../stores/useApprovalTasksStore';
import {
    getAssigneeNameById,
    getAvailableAssigneeOptions,
    getAvailableAssignees,
    getCurrentTimestamp,
    toClientPriority,
    toClientStatus,
    toServerPriority,
    toServerStatus,
} from '../utils/approval-task-utils';

import type {ApprovalTaskAttachmentI, ApprovalTaskCommentI, ApprovalTaskI, AssigneeOptionI} from '../types/types';

export interface UseApprovalTasksReturnI {
    addAttachmentToApprovalTask: (
        approvalTaskId: string,
        attachment: Omit<ApprovalTaskAttachmentI, 'id' | 'uploadedAt'>
    ) => void;
    addCommentToApprovalTask: (approvalTaskId: string, comment: Omit<ApprovalTaskCommentI, 'id' | 'timestamp'>) => void;
    availableAssignees: string[];
    availableAssigneeOptions: AssigneeOptionI[];
    removeAttachmentFromApprovalTask: (approvalTaskId: string, attachmentId: string) => void;
    approvalTasks: ApprovalTaskI[];
    selectedApprovalTaskId: string | null;
    selectedApprovalTaskObject: ApprovalTaskI | null;
    setSelectedApprovalTaskId: (approvalTaskId: string | null) => void;
    updateApprovalTask: (approvalTask: ApprovalTaskI) => void;
}

export function useApprovalTasks(): UseApprovalTasksReturnI {
    const {
        addAttachment: storeAddAttachment,
        addComment: storeAddComment,
        approvalTasks,
        removeAttachment: storeRemoveAttachment,
        selectedApprovalTaskId,
        setSelectedApprovalTaskId,
        updateApprovalTask: storeUpdateApprovalTask,
    } = useApprovalTasksStore(
        useShallow((state) => ({
            addAttachment: state.addAttachment,
            addComment: state.addComment,
            approvalTasks: state.approvalTasks,
            removeAttachment: state.removeAttachment,
            selectedApprovalTaskId: state.selectedApprovalTaskId,
            setSelectedApprovalTaskId: state.setSelectedApprovalTaskId,
            updateApprovalTask: state.updateApprovalTask,
        }))
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: approvalTasksData} = useApprovalTasksQuery({environmentId: currentEnvironmentId});
    const {data: usersData} = useUsersQuery();

    const updateApprovalTaskMutation = useUpdateApprovalTaskMutation({
        onError: (error) => {
            console.error('Error updating approval task:', error);
        },
    });

    const availableAssignees = useMemo(() => getAvailableAssignees(usersData?.users?.content), [usersData]);
    const availableAssigneeOptions = useMemo(() => getAvailableAssigneeOptions(usersData?.users?.content), [usersData]);

    const mapApiApprovalTaskToUiApprovalTask = useCallback(
        (apiApprovalTask: ApprovalTask): ApprovalTaskI => ({
            assignee: getAssigneeNameById(apiApprovalTask.assigneeId, usersData?.users?.content),
            assigneeId: apiApprovalTask.assigneeId,
            attachments: [],
            comments: [],
            createdAt: apiApprovalTask.createdDate || new Date().toISOString(),
            dependencies: [],
            description: apiApprovalTask.description || '',
            dueDate: apiApprovalTask.dueDate ?? undefined,
            id: apiApprovalTask.id,
            jobResumeId: apiApprovalTask.jobResumeId,
            priority: toClientPriority(apiApprovalTask.priority),
            status: toClientStatus(apiApprovalTask.status),
            title: apiApprovalTask.name,
            version: apiApprovalTask.version,
        }),
        [usersData]
    );

    useEffect(() => {
        if (approvalTasksData?.approvalTasks) {
            const mappedApprovalTasks = approvalTasksData.approvalTasks
                .filter((approvalTask): approvalTask is ApprovalTask => approvalTask !== null)
                .map(mapApiApprovalTaskToUiApprovalTask);

            useApprovalTasksStore.getState().setApprovalTasks(mappedApprovalTasks);
        }
    }, [approvalTasksData, mapApiApprovalTaskToUiApprovalTask]);

    const selectedApprovalTaskObject =
        approvalTasks.find((approvalTask) => approvalTask.id === selectedApprovalTaskId) || null;

    const generateCommentId = useCallback(() => {
        const allComments = approvalTasks.flatMap((approvalTask) => approvalTask.comments);
        const maxId = Math.max(...allComments.map((comment) => Number.parseInt(comment.id.replace('c', ''))), 0);

        return `c${maxId + 1}`;
    }, [approvalTasks]);

    const generateAttachmentId = useCallback(() => {
        const allAttachments = approvalTasks.flatMap((approvalTask) => approvalTask.attachments);
        const maxId = Math.max(
            ...allAttachments.map((attachment) => Number.parseInt(attachment.id.replace('a', ''))),
            0
        );

        return `a${maxId + 1}`;
    }, [approvalTasks]);

    const updateApprovalTask = useCallback(
        (updatedApprovalTask: ApprovalTaskI) => {
            updateApprovalTaskMutation.mutate(
                {
                    approvalTask: {
                        assigneeId: updatedApprovalTask.assigneeId,
                        description: updatedApprovalTask.description,
                        dueDate: updatedApprovalTask.dueDate ?? null,
                        id: updatedApprovalTask.id,
                        name: updatedApprovalTask.title,
                        priority: toServerPriority(updatedApprovalTask.priority),
                        status: toServerStatus(updatedApprovalTask.status),
                        version: updatedApprovalTask.version,
                    },
                },
                {
                    onSuccess: (data) => {
                        storeUpdateApprovalTask({
                            ...updatedApprovalTask,
                            version: data.updateApprovalTask?.version ?? updatedApprovalTask.version,
                        });
                    },
                }
            );
        },
        [storeUpdateApprovalTask, updateApprovalTaskMutation]
    );

    const addCommentToApprovalTask = useCallback(
        (approvalTaskId: string, commentData: Omit<ApprovalTaskCommentI, 'id' | 'timestamp'>) => {
            const comment: ApprovalTaskCommentI = {
                ...commentData,
                id: generateCommentId(),
                timestamp: getCurrentTimestamp(),
            };

            storeAddComment(approvalTaskId, comment);
        },
        [generateCommentId, storeAddComment]
    );

    const addAttachmentToApprovalTask = useCallback(
        (approvalTaskId: string, attachmentData: Omit<ApprovalTaskAttachmentI, 'id' | 'uploadedAt'>) => {
            const attachment: ApprovalTaskAttachmentI = {
                ...attachmentData,
                id: generateAttachmentId(),
                uploadedAt: getCurrentTimestamp(),
            };

            storeAddAttachment(approvalTaskId, attachment);
        },
        [generateAttachmentId, storeAddAttachment]
    );

    const removeAttachmentFromApprovalTask = useCallback(
        (approvalTaskId: string, attachmentId: string) => {
            storeRemoveAttachment(approvalTaskId, attachmentId);
        },
        [storeRemoveAttachment]
    );

    return {
        addAttachmentToApprovalTask,
        addCommentToApprovalTask,
        approvalTasks,
        availableAssigneeOptions,
        availableAssignees,
        removeAttachmentFromApprovalTask,
        selectedApprovalTaskId,
        selectedApprovalTaskObject,
        setSelectedApprovalTaskId,
        updateApprovalTask,
    };
}
