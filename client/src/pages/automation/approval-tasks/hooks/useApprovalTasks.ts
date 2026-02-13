import {
    ApprovalTask,
    useApprovalTasksQuery,
    useUpdateApprovalTaskMutation,
    useUsersQuery,
} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo} from 'react';

import {useApprovalTasksStore} from '../stores/useApprovalTasksStore';
import {getAvailableAssignees, getCurrentTimestamp} from '../utils/approval-task-utils';

import type {ApprovalTaskAttachmentI, ApprovalTaskCommentI, ApprovalTaskI} from '../types/types';

export interface UseApprovalTasksReturnI {
    addAttachmentToApprovalTask: (
        approvalTaskId: string,
        attachment: Omit<ApprovalTaskAttachmentI, 'id' | 'uploadedAt'>
    ) => void;
    addCommentToApprovalTask: (approvalTaskId: string, comment: Omit<ApprovalTaskCommentI, 'id' | 'timestamp'>) => void;
    availableAssignees: string[];
    removeAttachmentFromApprovalTask: (approvalTaskId: string, attachmentId: string) => void;
    approvalTasks: ApprovalTaskI[];
    selectedApprovalTaskId: string | null;
    selectedApprovalTaskObject: ApprovalTaskI | null;
    setSelectedApprovalTaskId: (approvalTaskId: string | null) => void;
    updateApprovalTask: (approvalTask: ApprovalTaskI) => void;
}

export function useApprovalTasks(): UseApprovalTasksReturnI {
    const approvalTasks = useApprovalTasksStore((state) => state.approvalTasks);
    const selectedApprovalTaskId = useApprovalTasksStore((state) => state.selectedApprovalTaskId);
    const setApprovalTasks = useApprovalTasksStore((state) => state.setApprovalTasks);
    const setSelectedApprovalTaskId = useApprovalTasksStore((state) => state.setSelectedApprovalTaskId);
    const storeUpdateApprovalTask = useApprovalTasksStore((state) => state.updateApprovalTask);
    const storeAddComment = useApprovalTasksStore((state) => state.addComment);
    const storeAddAttachment = useApprovalTasksStore((state) => state.addAttachment);
    const storeRemoveAttachment = useApprovalTasksStore((state) => state.removeAttachment);

    const {data: approvalTasksData} = useApprovalTasksQuery();
    const {data: usersData} = useUsersQuery();

    const updateApprovalTaskMutation = useUpdateApprovalTaskMutation({
        onError: (error) => {
            console.error('Error updating approval task:', error);
        },
    });

    const availableAssignees = useMemo(() => getAvailableAssignees(usersData?.users?.content), [usersData]);

    const mapApiApprovalTaskToUiApprovalTask = useCallback(
        (apiApprovalTask: ApprovalTask): ApprovalTaskI => ({
            assignee: apiApprovalTask.createdBy || 'Unassigned',
            attachments: [],
            comments: [],
            createdAt: apiApprovalTask.createdDate || new Date().toISOString(),
            dependencies: [],
            description: apiApprovalTask.description || '',
            dueDate: undefined,
            id: apiApprovalTask.id,
            priority: 'medium',
            status: 'open',
            title: apiApprovalTask.name,
            version: apiApprovalTask.version,
        }),
        []
    );

    useEffect(() => {
        if (approvalTasksData?.approvalTasks) {
            const mappedApprovalTasks = approvalTasksData.approvalTasks
                .filter((approvalTask): approvalTask is ApprovalTask => approvalTask !== null)
                .map(mapApiApprovalTaskToUiApprovalTask);

            setApprovalTasks(mappedApprovalTasks);
        }
    }, [approvalTasksData, mapApiApprovalTaskToUiApprovalTask, setApprovalTasks]);

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
            storeUpdateApprovalTask(updatedApprovalTask);

            updateApprovalTaskMutation.mutate({
                approvalTask: {
                    description: updatedApprovalTask.description,
                    id: updatedApprovalTask.id,
                    name: updatedApprovalTask.title,
                    version: updatedApprovalTask.version,
                },
            });
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
        availableAssignees,
        removeAttachmentFromApprovalTask,
        selectedApprovalTaskId,
        selectedApprovalTaskObject,
        setSelectedApprovalTaskId,
        updateApprovalTask,
    };
}
