import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

import {formatDate, getPriorityColor, getStatusIcon, highlightText} from '../utils/approval-task-utils';

import type {ReactNode} from 'react';

import type {ApprovalTaskAttachmentI, ApprovalTaskCommentI, ApprovalTaskI, FiltersI} from '../types/types';

const DEFAULT_FILTERS: FiltersI = {
    assignee: 'all',
    priority: 'all',
    status: 'all',
};

interface ApprovalTasksStateI {
    addApprovalTask: (approvalTask: ApprovalTaskI) => void;
    addAttachment: (approvalTaskId: string, attachment: ApprovalTaskAttachmentI) => void;
    addComment: (approvalTaskId: string, comment: ApprovalTaskCommentI) => void;
    approvalTasks: ApprovalTaskI[];
    cycleApprovalTaskStatus: (approvalTaskId: string) => void;
    deleteApprovalTask: (approvalTaskId: string) => void;
    filters: FiltersI;
    getFormattedDueDate: (approvalTask: ApprovalTaskI) => string | null;
    getHighlightedAssignee: (approvalTask: ApprovalTaskI) => ReactNode;
    getHighlightedDescription: (approvalTask: ApprovalTaskI) => ReactNode;
    getHighlightedTitle: (approvalTask: ApprovalTaskI) => ReactNode;
    getPriorityColor: (approvalTask: ApprovalTaskI) => string;
    getSelectedApprovalTask: () => ApprovalTaskI | null;
    getStatusIcon: (approvalTask: ApprovalTaskI) => ReactNode;
    hasActiveFilters: () => boolean;
    removeAttachment: (approvalTaskId: string, attachmentId: string) => void;
    resetFilters: () => void;
    searchQuery: string;
    selectedApprovalTaskId: string | null;
    setApprovalTasks: (approvalTasks: ApprovalTaskI[]) => void;
    setFilters: (filters: FiltersI | ((prev: FiltersI) => FiltersI)) => void;
    setSearchQuery: (query: string) => void;
    setSelectedApprovalTaskId: (approvalTaskId: string | null) => void;
    updateApprovalTask: (approvalTask: ApprovalTaskI) => void;
}

const cycleStatus = (currentStatus: string): 'open' | 'in-progress' | 'completed' => {
    switch (currentStatus) {
        case 'open':
            return 'in-progress';
        case 'in-progress':
            return 'completed';
        case 'completed':
            return 'open';
        default:
            return 'open';
    }
};

export const useApprovalTasksStore = create<ApprovalTasksStateI>()(
    devtools(
        (set, get) => ({
            addApprovalTask: (approvalTask: ApprovalTaskI) => {
                set((state) => ({approvalTasks: [approvalTask, ...state.approvalTasks]}));
            },

            addAttachment: (approvalTaskId: string, attachment: ApprovalTaskAttachmentI) => {
                set((state) => ({
                    approvalTasks: state.approvalTasks.map((approvalTask) => {
                        if (approvalTask.id === approvalTaskId) {
                            return {...approvalTask, attachments: [...approvalTask.attachments, attachment]};
                        }

                        return approvalTask;
                    }),
                }));
            },

            addComment: (approvalTaskId: string, comment: ApprovalTaskCommentI) => {
                set((state) => ({
                    approvalTasks: state.approvalTasks.map((approvalTask) => {
                        if (approvalTask.id === approvalTaskId) {
                            return {...approvalTask, comments: [...approvalTask.comments, comment]};
                        }

                        return approvalTask;
                    }),
                }));
            },

            approvalTasks: [],

            cycleApprovalTaskStatus: (approvalTaskId: string) => {
                set((state) => ({
                    approvalTasks: state.approvalTasks.map((approvalTask) => {
                        if (approvalTask.id === approvalTaskId) {
                            return {...approvalTask, status: cycleStatus(approvalTask.status)};
                        }

                        return approvalTask;
                    }),
                }));
            },

            deleteApprovalTask: (approvalTaskId: string) => {
                set((state) => ({
                    approvalTasks: state.approvalTasks.filter((approvalTask) => approvalTask.id !== approvalTaskId),
                    selectedApprovalTaskId:
                        state.selectedApprovalTaskId === approvalTaskId ? null : state.selectedApprovalTaskId,
                }));
            },

            filters: DEFAULT_FILTERS,

            getFormattedDueDate: (approvalTask: ApprovalTaskI) =>
                approvalTask.dueDate ? formatDate(approvalTask.dueDate) : null,

            getHighlightedAssignee: (approvalTask: ApprovalTaskI) => {
                const {searchQuery} = get();

                return searchQuery ? highlightText(approvalTask.assignee, searchQuery) : approvalTask.assignee;
            },

            getHighlightedDescription: (approvalTask: ApprovalTaskI) => {
                const {searchQuery} = get();

                return searchQuery ? highlightText(approvalTask.description, searchQuery) : approvalTask.description;
            },

            getHighlightedTitle: (approvalTask: ApprovalTaskI) => {
                const {searchQuery} = get();

                return searchQuery ? highlightText(approvalTask.title, searchQuery) : approvalTask.title;
            },

            getPriorityColor: (approvalTask: ApprovalTaskI) => getPriorityColor(approvalTask.priority),

            getSelectedApprovalTask: () => {
                const {approvalTasks, selectedApprovalTaskId} = get();

                if (!selectedApprovalTaskId) {
                    return null;
                }

                return approvalTasks.find((approvalTask) => approvalTask.id === selectedApprovalTaskId) || null;
            },

            getStatusIcon: (approvalTask: ApprovalTaskI) => getStatusIcon(approvalTask.status),

            hasActiveFilters: () => {
                const {filters} = get();

                return filters.status !== 'all' || filters.priority !== 'all' || filters.assignee !== 'all';
            },

            removeAttachment: (approvalTaskId: string, attachmentId: string) => {
                set((state) => ({
                    approvalTasks: state.approvalTasks.map((approvalTask) => {
                        if (approvalTask.id === approvalTaskId) {
                            return {
                                ...approvalTask,
                                attachments: approvalTask.attachments.filter(
                                    (attachment) => attachment.id !== attachmentId
                                ),
                            };
                        }

                        return approvalTask;
                    }),
                }));
            },

            resetFilters: () => {
                set(() => ({filters: DEFAULT_FILTERS}));
            },

            searchQuery: '',

            selectedApprovalTaskId: null,

            setApprovalTasks: (approvalTasks: ApprovalTaskI[]) => {
                set(() => ({approvalTasks}));
            },

            setFilters: (filtersOrUpdater: FiltersI | ((prev: FiltersI) => FiltersI)) => {
                set((state) => ({
                    filters:
                        typeof filtersOrUpdater === 'function' ? filtersOrUpdater(state.filters) : filtersOrUpdater,
                }));
            },

            setSearchQuery: (query: string) => {
                set(() => ({searchQuery: query}));
            },

            setSelectedApprovalTaskId: (approvalTaskId: string | null) => {
                set(() => ({selectedApprovalTaskId: approvalTaskId}));
            },

            updateApprovalTask: (updatedApprovalTask: ApprovalTaskI) => {
                set((state) => ({
                    approvalTasks: state.approvalTasks.map((approvalTask) =>
                        approvalTask.id === updatedApprovalTask.id ? updatedApprovalTask : approvalTask
                    ),
                }));
            },
        }),
        {
            name: 'bytechef.approvalTasks',
        }
    )
);
