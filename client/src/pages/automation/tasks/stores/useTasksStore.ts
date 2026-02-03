import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

import {formatDate, getPriorityColor, getStatusIcon, highlightText} from '../utils/task-utils';

import type {ReactNode} from 'react';

import type {FiltersI, TaskAttachmentI, TaskCommentI, TaskI} from '../types/types';

const DEFAULT_FILTERS: FiltersI = {
    assignee: 'all',
    priority: 'all',
    status: 'all',
};

interface TasksStateI {
    addAttachment: (taskId: string, attachment: TaskAttachmentI) => void;
    addComment: (taskId: string, comment: TaskCommentI) => void;
    addTask: (task: TaskI) => void;
    cycleTaskStatus: (taskId: string) => void;
    deleteTask: (taskId: string) => void;
    filters: FiltersI;
    getFormattedDueDate: (task: TaskI) => string | null;
    getHighlightedAssignee: (task: TaskI) => ReactNode;
    getHighlightedDescription: (task: TaskI) => ReactNode;
    getHighlightedTitle: (task: TaskI) => ReactNode;
    getPriorityColor: (task: TaskI) => string;
    getSelectedTask: () => TaskI | null;
    getStatusIcon: (task: TaskI) => ReactNode;
    hasActiveFilters: () => boolean;
    removeAttachment: (taskId: string, attachmentId: string) => void;
    resetFilters: () => void;
    searchQuery: string;
    selectedTaskId: string | null;
    setFilters: (filters: FiltersI | ((prev: FiltersI) => FiltersI)) => void;
    setSearchQuery: (query: string) => void;
    setSelectedTaskId: (taskId: string | null) => void;
    setTasks: (tasks: TaskI[]) => void;
    tasks: TaskI[];
    updateTask: (task: TaskI) => void;
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

export const useTasksStore = create<TasksStateI>()(
    devtools(
        (set, get) => ({
            addAttachment: (taskId: string, attachment: TaskAttachmentI) => {
                set((state) => ({
                    tasks: state.tasks.map((task) => {
                        if (task.id === taskId) {
                            return {...task, attachments: [...task.attachments, attachment]};
                        }

                        return task;
                    }),
                }));
            },

            addComment: (taskId: string, comment: TaskCommentI) => {
                set((state) => ({
                    tasks: state.tasks.map((task) => {
                        if (task.id === taskId) {
                            return {...task, comments: [...task.comments, comment]};
                        }

                        return task;
                    }),
                }));
            },

            addTask: (task: TaskI) => {
                set((state) => ({tasks: [task, ...state.tasks]}));
            },

            cycleTaskStatus: (taskId: string) => {
                set((state) => ({
                    tasks: state.tasks.map((task) => {
                        if (task.id === taskId) {
                            return {...task, status: cycleStatus(task.status)};
                        }

                        return task;
                    }),
                }));
            },

            deleteTask: (taskId: string) => {
                set((state) => ({
                    selectedTaskId: state.selectedTaskId === taskId ? null : state.selectedTaskId,
                    tasks: state.tasks.filter((task) => task.id !== taskId),
                }));
            },

            filters: DEFAULT_FILTERS,

            getFormattedDueDate: (task: TaskI) => (task.dueDate ? formatDate(task.dueDate) : null),

            getHighlightedAssignee: (task: TaskI) => {
                const {searchQuery} = get();

                return searchQuery ? highlightText(task.assignee, searchQuery) : task.assignee;
            },

            getHighlightedDescription: (task: TaskI) => {
                const {searchQuery} = get();

                return searchQuery ? highlightText(task.description, searchQuery) : task.description;
            },

            getHighlightedTitle: (task: TaskI) => {
                const {searchQuery} = get();

                return searchQuery ? highlightText(task.title, searchQuery) : task.title;
            },

            getPriorityColor: (task: TaskI) => getPriorityColor(task.priority),

            getSelectedTask: () => {
                const {selectedTaskId, tasks} = get();

                if (!selectedTaskId) {
                    return null;
                }

                return tasks.find((task) => task.id === selectedTaskId) || null;
            },

            getStatusIcon: (task: TaskI) => getStatusIcon(task.status),

            hasActiveFilters: () => {
                const {filters} = get();

                return filters.status !== 'all' || filters.priority !== 'all' || filters.assignee !== 'all';
            },

            removeAttachment: (taskId: string, attachmentId: string) => {
                set((state) => ({
                    tasks: state.tasks.map((task) => {
                        if (task.id === taskId) {
                            return {
                                ...task,
                                attachments: task.attachments.filter((attachment) => attachment.id !== attachmentId),
                            };
                        }

                        return task;
                    }),
                }));
            },

            resetFilters: () => {
                set(() => ({filters: DEFAULT_FILTERS}));
            },

            searchQuery: '',

            selectedTaskId: null,

            setFilters: (filtersOrUpdater: FiltersI | ((prev: FiltersI) => FiltersI)) => {
                set((state) => ({
                    filters:
                        typeof filtersOrUpdater === 'function' ? filtersOrUpdater(state.filters) : filtersOrUpdater,
                }));
            },

            setSearchQuery: (query: string) => {
                set(() => ({searchQuery: query}));
            },

            setSelectedTaskId: (taskId: string | null) => {
                set(() => ({selectedTaskId: taskId}));
            },

            setTasks: (tasks: TaskI[]) => {
                set(() => ({tasks}));
            },

            tasks: [],

            updateTask: (updatedTask: TaskI) => {
                set((state) => ({
                    tasks: state.tasks.map((task) => (task.id === updatedTask.id ? updatedTask : task)),
                }));
            },
        }),
        {
            name: 'bytechef.tasks',
        }
    )
);
