import {useUpdateApprovalTaskMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {ApprovalTaskI} from '../../types/types';
import {formatDate, getAvailableAssignees, getPriorityColor, getStatusIcon} from '../../utils/approval-task-utils';

import type {ReactNode} from 'react';

export interface UseApprovalTaskDetailReturnI {
    allApprovalTasks: ApprovalTaskI[];
    availableAssignees: string[];
    createdAtFormatted: string;
    displayApprovalTask: ApprovalTaskI | null;
    handleCancel: () => void;
    handleEdit: () => void;
    handleFieldChange: (field: keyof ApprovalTaskI, value: string | string[]) => void;
    handleSave: () => void;
    isEditing: boolean;
    priorityColor: string;
    approvalTask: ApprovalTaskI | null;
    statusIcon: ReactNode;
}

export function useApprovalTaskDetail(): UseApprovalTaskDetailReturnI {
    const [isEditing, setIsEditing] = useState(false);
    const [editingApprovalTask, setEditingApprovalTask] = useState<ApprovalTaskI | null>(null);

    const {approvalTasks, selectedApprovalTask, updateApprovalTask} = useApprovalTasksStore(
        useShallow((state) => ({
            approvalTasks: state.approvalTasks,
            selectedApprovalTask: state.getSelectedApprovalTask(),
            updateApprovalTask: state.updateApprovalTask,
        }))
    );

    const {data: usersData} = useUsersQuery();

    const availableAssignees = useMemo(() => getAvailableAssignees(usersData?.users?.content), [usersData]);

    const displayApprovalTask = isEditing ? editingApprovalTask : selectedApprovalTask;

    const createdAtFormatted = useMemo(
        () => (displayApprovalTask ? formatDate(displayApprovalTask.createdAt) : ''),
        [displayApprovalTask]
    );

    const priorityColor = useMemo(
        () => (displayApprovalTask ? getPriorityColor(displayApprovalTask.priority) : ''),
        [displayApprovalTask]
    );

    const statusIcon = useMemo(
        () => (displayApprovalTask ? getStatusIcon(displayApprovalTask.status) : null),
        [displayApprovalTask]
    );

    const updateApprovalTaskMutation = useUpdateApprovalTaskMutation({
        onError: (error) => {
            console.error('Error updating approval task:', error);
        },
    });

    const handleEditApprovalTask = useCallback(() => {
        if (selectedApprovalTask) {
            setEditingApprovalTask({...selectedApprovalTask});
            setIsEditing(true);
        }
    }, [selectedApprovalTask]);

    const handleSaveApprovalTask = useCallback(() => {
        if (editingApprovalTask) {
            updateApprovalTask(editingApprovalTask);

            updateApprovalTaskMutation.mutate({
                approvalTask: {
                    description: editingApprovalTask.description,
                    id: editingApprovalTask.id,
                    name: editingApprovalTask.title,
                    version: editingApprovalTask.version,
                },
            });

            setIsEditing(false);
            setEditingApprovalTask(null);
        }
    }, [editingApprovalTask, updateApprovalTask, updateApprovalTaskMutation]);

    const handleCancelEdit = useCallback(() => {
        setIsEditing(false);
        setEditingApprovalTask(null);
    }, []);

    const handleApprovalTaskFieldChange = useCallback((field: keyof ApprovalTaskI, value: string | string[]) => {
        setEditingApprovalTask((prevApprovalTask) => {
            if (prevApprovalTask) {
                return {
                    ...prevApprovalTask,
                    [field]: value,
                };
            }

            return prevApprovalTask;
        });
    }, []);

    return {
        allApprovalTasks: approvalTasks,
        approvalTask: selectedApprovalTask,
        availableAssignees,
        createdAtFormatted,
        displayApprovalTask,
        handleCancel: handleCancelEdit,
        handleEdit: handleEditApprovalTask,
        handleFieldChange: handleApprovalTaskFieldChange,
        handleSave: handleSaveApprovalTask,
        isEditing,
        priorityColor,
        statusIcon,
    };
}
