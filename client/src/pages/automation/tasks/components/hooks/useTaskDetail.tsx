import {useUpdateTaskMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useTasksStore} from '../../stores/useTasksStore';
import {TaskI} from '../../types/types';
import {formatDate, getAvailableAssignees, getPriorityColor, getStatusIcon} from '../../utils/task-utils';

import type React from 'react';

export interface UseTaskDetailReturnI {
    // Task data
    task: TaskI | null;
    allTasks: TaskI[];
    availableAssignees: string[];

    // Computed display values
    createdAtFormatted: string;
    displayTask: TaskI | null;
    priorityColor: string;
    statusIcon: React.ReactNode;

    // Editing state
    isEditing: boolean;
    handleCancel: () => void;
    handleEdit: () => void;
    handleFieldChange: (field: keyof TaskI, value: string | string[]) => void;
    handleSave: () => void;
}

export function useTaskDetail(): UseTaskDetailReturnI {
    const [isEditing, setIsEditing] = useState(false);
    const [editingTask, setEditingTask] = useState<TaskI | null>(null);

    const {selectedTask, tasks, updateTask} = useTasksStore(
        useShallow((state) => ({
            selectedTask: state.getSelectedTask(),
            tasks: state.tasks,
            updateTask: state.updateTask,
        }))
    );

    // Fetch users for assignee selection
    const {data: usersData} = useUsersQuery();

    const availableAssignees = useMemo(() => getAvailableAssignees(usersData?.users?.content), [usersData]);

    const displayTask = isEditing ? editingTask : selectedTask;

    const createdAtFormatted = useMemo(() => (displayTask ? formatDate(displayTask.createdAt) : ''), [displayTask]);

    const priorityColor = useMemo(() => (displayTask ? getPriorityColor(displayTask.priority) : ''), [displayTask]);

    const statusIcon = useMemo(() => (displayTask ? getStatusIcon(displayTask.status) : null), [displayTask]);

    // API mutation
    const updateTaskMutation = useUpdateTaskMutation({
        onError: (error) => {
            console.error('Error updating task:', error);
        },
    });

    // Editing handlers
    const handleEditTask = useCallback(() => {
        if (selectedTask) {
            setEditingTask({...selectedTask});
            setIsEditing(true);
        }
    }, [selectedTask]);

    const handleSaveTask = useCallback(() => {
        if (editingTask) {
            updateTask(editingTask);

            updateTaskMutation.mutate({
                task: {
                    description: editingTask.description,
                    id: editingTask.id,
                    name: editingTask.title,
                    version: editingTask.version,
                },
            });

            setIsEditing(false);
            setEditingTask(null);
        }
    }, [editingTask, updateTask, updateTaskMutation]);

    const handleCancelEdit = useCallback(() => {
        setIsEditing(false);
        setEditingTask(null);
    }, []);

    const handleTaskFieldChange = useCallback((field: keyof TaskI, value: string | string[]) => {
        setEditingTask((prevTask) => {
            if (prevTask) {
                return {
                    ...prevTask,
                    [field]: value,
                };
            }

            return prevTask;
        });
    }, []);

    return {
        allTasks: tasks,
        availableAssignees,
        createdAtFormatted,
        displayTask,
        handleCancel: handleCancelEdit,
        handleEdit: handleEditTask,
        handleFieldChange: handleTaskFieldChange,
        handleSave: handleSaveTask,
        isEditing,
        priorityColor,
        statusIcon,
        task: selectedTask,
    };
}
