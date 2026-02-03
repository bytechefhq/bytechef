import {Task, useTasksQuery, useUpdateTaskMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo} from 'react';

import {useTasksStore} from '../stores/useTasksStore';
import {getAvailableAssignees, getCurrentTimestamp} from '../utils/task-utils';

import type {TaskAttachmentI, TaskCommentI, TaskI} from '../types/types';

export interface UseTasksReturnI {
    // Task data
    tasks: TaskI[];
    selectedTaskId: string | null;
    selectedTaskObject: TaskI | null;

    // Selection
    setSelectedTaskId: (taskId: string | null) => void;

    // Task mutations
    updateTask: (task: TaskI) => void;

    // Comment/Attachment mutations
    addCommentToTask: (taskId: string, comment: Omit<TaskCommentI, 'id' | 'timestamp'>) => void;
    addAttachmentToTask: (taskId: string, attachment: Omit<TaskAttachmentI, 'id' | 'uploadedAt'>) => void;
    removeAttachmentFromTask: (taskId: string, attachmentId: string) => void;

    // Constants
    availableAssignees: string[];
}

export function useTasks(): UseTasksReturnI {
    const tasks = useTasksStore((state) => state.tasks);
    const selectedTaskId = useTasksStore((state) => state.selectedTaskId);
    const setTasks = useTasksStore((state) => state.setTasks);
    const setSelectedTaskId = useTasksStore((state) => state.setSelectedTaskId);
    const storeUpdateTask = useTasksStore((state) => state.updateTask);
    const storeAddComment = useTasksStore((state) => state.addComment);
    const storeAddAttachment = useTasksStore((state) => state.addAttachment);
    const storeRemoveAttachment = useTasksStore((state) => state.removeAttachment);

    const {data: tasksData} = useTasksQuery();
    const {data: usersData} = useUsersQuery();

    const updateTaskMutation = useUpdateTaskMutation({
        onError: (error) => {
            console.error('Error updating task:', error);
        },
    });

    const availableAssignees = useMemo(() => getAvailableAssignees(usersData?.users?.content), [usersData]);

    const mapApiTaskToUiTask = useCallback(
        (apiTask: Task): TaskI => ({
            assignee: apiTask.createdBy || 'Unassigned',
            attachments: [],
            comments: [],
            createdAt: apiTask.createdDate || new Date().toISOString(),
            dependencies: [],
            description: apiTask.description || '',
            dueDate: undefined,
            id: apiTask.id,
            priority: 'medium',
            status: 'open',
            title: apiTask.name,
            version: apiTask.version,
        }),
        []
    );

    useEffect(() => {
        if (tasksData?.tasks) {
            const mappedTasks = tasksData.tasks.filter((task): task is Task => task !== null).map(mapApiTaskToUiTask);

            setTasks(mappedTasks);
        }
    }, [tasksData, mapApiTaskToUiTask, setTasks]);

    const selectedTaskObject = tasks.find((task) => task.id === selectedTaskId) || null;

    const generateCommentId = useCallback(() => {
        const allComments = tasks.flatMap((task) => task.comments);
        const maxId = Math.max(...allComments.map((comment) => Number.parseInt(comment.id.replace('c', ''))), 0);

        return `c${maxId + 1}`;
    }, [tasks]);

    const generateAttachmentId = useCallback(() => {
        const allAttachments = tasks.flatMap((task) => task.attachments);
        const maxId = Math.max(
            ...allAttachments.map((attachment) => Number.parseInt(attachment.id.replace('a', ''))),
            0
        );

        return `a${maxId + 1}`;
    }, [tasks]);

    const updateTask = useCallback(
        (updatedTask: TaskI) => {
            storeUpdateTask(updatedTask);

            updateTaskMutation.mutate({
                task: {
                    description: updatedTask.description,
                    id: updatedTask.id,
                    name: updatedTask.title,
                    version: updatedTask.version,
                },
            });
        },
        [storeUpdateTask, updateTaskMutation]
    );

    const addCommentToTask = useCallback(
        (taskId: string, commentData: Omit<TaskCommentI, 'id' | 'timestamp'>) => {
            const comment: TaskCommentI = {
                ...commentData,
                id: generateCommentId(),
                timestamp: getCurrentTimestamp(),
            };

            storeAddComment(taskId, comment);
        },
        [generateCommentId, storeAddComment]
    );

    const addAttachmentToTask = useCallback(
        (taskId: string, attachmentData: Omit<TaskAttachmentI, 'id' | 'uploadedAt'>) => {
            const attachment: TaskAttachmentI = {
                ...attachmentData,
                id: generateAttachmentId(),
                uploadedAt: getCurrentTimestamp(),
            };

            storeAddAttachment(taskId, attachment);
        },
        [generateAttachmentId, storeAddAttachment]
    );

    const removeAttachmentFromTask = useCallback(
        (taskId: string, attachmentId: string) => {
            storeRemoveAttachment(taskId, attachmentId);
        },
        [storeRemoveAttachment]
    );

    return {
        addAttachmentToTask,
        addCommentToTask,
        availableAssignees,
        removeAttachmentFromTask,
        selectedTaskId,
        selectedTaskObject,
        setSelectedTaskId,
        tasks,
        updateTask,
    };
}
