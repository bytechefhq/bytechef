import {useCreateApprovalTaskMutation, useUsersQuery} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useState} from 'react';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {
    getAssigneeNameById,
    getAvailableAssigneeOptions,
    toServerPriority,
    toServerStatus,
} from '../../utils/approval-task-utils';

import type {ApprovalTaskI, AssigneeOptionI, NewApprovalTaskFormI} from '../../types/types';

const INITIAL_FORM_STATE: NewApprovalTaskFormI = {
    assignee: '',
    assigneeId: '',
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

export interface UseApprovalTaskCreateDialogReturnI {
    availableAssigneeOptions: AssigneeOptionI[];
    errors: Partial<NewApprovalTaskFormI>;
    form: NewApprovalTaskFormI;
    handleAssigneeChange: (assigneeId: string) => void;
    handleCloseDialog: () => void;
    handleFormChange: (field: keyof NewApprovalTaskFormI, value: string | string[]) => void;
    handleOpenChange: (open: boolean) => void;
    handleOpenDialog: () => void;
    handleSubmit: () => void;
    isOpen: boolean;
}

export function useApprovalTaskCreateDialog(): UseApprovalTaskCreateDialogReturnI {
    const [isOpen, setIsOpen] = useState(false);
    const [form, setForm] = useState<NewApprovalTaskFormI>(INITIAL_FORM_STATE);
    const [errors, setErrors] = useState<Partial<NewApprovalTaskFormI>>({});

    const addApprovalTask = useApprovalTasksStore((state) => state.addApprovalTask);

    const {data: usersData} = useUsersQuery();

    const availableAssigneeOptions = useMemo(() => getAvailableAssigneeOptions(usersData?.users?.content), [usersData]);

    const createApprovalTaskMutation = useCreateApprovalTaskMutation({
        onError: (error) => {
            console.error('Error creating approval task:', error);
        },
    });

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

    const handleFormChange = useCallback((field: keyof NewApprovalTaskFormI, value: string | string[]) => {
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

    const handleAssigneeChange = useCallback(
        (assigneeId: string) => {
            const assigneeName = getAssigneeNameById(assigneeId, usersData?.users?.content);

            setForm((previousForm) => ({
                ...previousForm,
                assignee: assigneeName,
                assigneeId,
            }));

            setErrors((previousErrors) => {
                const updatedErrors = {...previousErrors};

                delete updatedErrors.assignee;
                delete updatedErrors.assigneeId;

                return updatedErrors;
            });
        },
        [usersData]
    );

    const isFormValid = useCallback((): boolean => {
        const validationErrors: Partial<NewApprovalTaskFormI> = {};

        if (!form.title.trim()) {
            validationErrors.title = 'Title is required';
        }

        if (!form.description.trim()) {
            validationErrors.description = 'Description is required';
        }

        if (!form.assigneeId) {
            validationErrors.assignee = 'Assignee is required';
        }

        setErrors(validationErrors);

        return Object.keys(validationErrors).length === 0;
    }, [form]);

    const handleSubmit = useCallback(() => {
        if (isFormValid()) {
            const newApprovalTask: ApprovalTaskI = {
                assignee: form.assignee,
                assigneeId: form.assigneeId,
                attachments: [],
                comments: [],
                createdAt: getCurrentDate(),
                dependencies: form.dependencies,
                description: form.description,
                dueDate: form.dueDate || undefined,
                priority: form.priority,
                status: form.status,
                title: form.title,
            };

            addApprovalTask(newApprovalTask);

            createApprovalTaskMutation.mutate({
                approvalTask: {
                    assigneeId: form.assigneeId || null,
                    description: form.description,
                    name: form.title,
                    priority: toServerPriority(form.priority),
                    status: toServerStatus(form.status),
                },
            });

            handleCloseDialog();
        }
    }, [form, isFormValid, addApprovalTask, createApprovalTaskMutation, handleCloseDialog]);

    return {
        availableAssigneeOptions,
        errors,
        form,
        handleAssigneeChange,
        handleCloseDialog,
        handleFormChange,
        handleOpenChange,
        handleOpenDialog,
        handleSubmit,
        isOpen,
    };
}
