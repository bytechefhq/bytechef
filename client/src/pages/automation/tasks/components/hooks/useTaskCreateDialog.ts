import {useCreateTaskMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useState} from 'react';

import {useTasksStore} from '../../stores/useTasksStore';

import type {NewTaskFormI, TaskI} from '../../types/types';

const INITIAL_FORM_STATE: NewTaskFormI = {
    assignee: '',
    dependencies: [],
    description: '',
    dueDate: '',
    priority: 'medium',
    status: 'open',
    templateId: 'none',
    title: '',
};

const getCurrentDate = (): string => {
    return new Date().toISOString().split('T')[0];
};

export interface UseTaskCreateDialogReturnI {
    // Dialog state
    isOpen: boolean;
    handleCloseDialog: () => void;
    handleOpenChange: (open: boolean) => void;
    handleOpenDialog: () => void;

    // Form state
    form: NewTaskFormI;
    errors: Partial<NewTaskFormI>;
    handleFormChange: (field: keyof NewTaskFormI, value: string | string[]) => void;

    // Actions
    handleSubmit: () => void;

    // Data for the dialog
    availableAssignees: string[];
}

export function useTaskCreateDialog(): UseTaskCreateDialogReturnI {
    const [isOpen, setIsOpen] = useState(false);
    const [form, setForm] = useState<NewTaskFormI>(INITIAL_FORM_STATE);
    const [errors, setErrors] = useState<Partial<NewTaskFormI>>({});

    const tasks = useTasksStore((state) => state.tasks);
    const addTask = useTasksStore((state) => state.addTask);

    // Fetch users for assignee selection
    const {data: usersData} = useUsersQuery();

    const availableAssignees = useMemo(() => {
        if (!usersData?.users?.content) {
            return [];
        }

        return usersData.users.content
            .filter((user) => user?.activated)
            .map((user) => {
                if (user?.firstName && user.lastName) {
                    return `${user.firstName} ${user.lastName}`;
                }

                return user?.login || user?.email || '';
            });
    }, [usersData]);

    // API mutation
    const createTaskMutation = useCreateTaskMutation({
        onError: (error) => {
            console.error('Error creating task:', error);
        },
    });

    // Generate unique task ID
    const generateTaskId = useCallback(() => {
        return (Math.max(...tasks.map((task) => Number.parseInt(task.id)), 0) + 1).toString();
    }, [tasks]);

    const handleOpenDialog = useCallback(() => {
        setIsOpen(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setForm(INITIAL_FORM_STATE);
        setErrors({});
        setIsOpen(false);
    }, []);

    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (open) {
                handleOpenDialog();
            } else {
                handleCloseDialog();
            }
        },
        [handleOpenDialog, handleCloseDialog]
    );

    const handleFormChange = useCallback((field: keyof NewTaskFormI, value: string | string[]) => {
        setForm((previousForm) => ({
            ...previousForm,
            [field]: value,
        }));

        setErrors((previousErrors) => {
            const updatedErrors = {...previousErrors};

            delete updatedErrors[field];

            return updatedErrors;
        });
    }, []);

    const validateForm = useCallback((): boolean => {
        const validationErrors: Partial<NewTaskFormI> = {};

        if (!form.title.trim()) {
            validationErrors.title = 'Title is required';
        }

        if (!form.description.trim()) {
            validationErrors.description = 'Description is required';
        }

        if (!form.assignee.trim()) {
            validationErrors.assignee = 'Assignee is required';
        }

        setErrors(validationErrors);

        return Object.keys(validationErrors).length === 0;
    }, [form]);

    const handleSubmit = useCallback(() => {
        if (validateForm()) {
            const newTask: TaskI = {
                assignee: form.assignee,
                attachments: [],
                comments: [],
                createdAt: getCurrentDate(),
                dependencies: form.dependencies,
                description: form.description,
                dueDate: form.dueDate || undefined,
                id: generateTaskId(),
                priority: form.priority,
                status: form.status,
                title: form.title,
            };

            addTask(newTask);

            createTaskMutation.mutate({
                task: {
                    description: form.description,
                    name: form.title,
                },
            });

            handleCloseDialog();
        }
    }, [form, validateForm, generateTaskId, addTask, createTaskMutation, handleCloseDialog]);

    return {
        availableAssignees,
        errors,
        form,
        handleCloseDialog,
        handleFormChange,
        handleOpenChange,
        handleOpenDialog,
        handleSubmit,
        isOpen,
    };
}
