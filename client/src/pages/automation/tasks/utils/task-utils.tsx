import {CheckCircle2, Circle, Clock} from 'lucide-react';

import type {SortDirectionType, SortOptionType, TaskI} from '../types/types';

const PRIORITY_ORDER: Record<string, number> = {high: 3, low: 1, medium: 2};
const STATUS_ORDER: Record<string, number> = {completed: 3, 'in-progress': 2, open: 1};

export const getCurrentTimestamp = (): string => {
    return new Date().toISOString();
};

export const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString('en-US', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
    });
};

export const formatTimestamp = (timestamp: string): string => {
    return new Date(timestamp).toLocaleString('en-US', {
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        month: 'short',
    });
};

export const getInitials = (name: string): string => {
    return name
        .split(' ')
        .map((namePart) => namePart[0])
        .join('')
        .toUpperCase();
};

export const getStatusIcon = (status: string): React.ReactNode => {
    switch (status) {
        case 'completed':
            return <CheckCircle2 className="size-4 text-green-500" />;
        case 'in-progress':
            return <Clock className="size-4 text-blue-500" />;
        default:
            return <Circle className="size-4 text-gray-400" />;
    }
};

export const getPriorityColor = (priority: string): string => {
    switch (priority) {
        case 'high':
            return 'bg-red-100 text-red-800 border-red-200';
        case 'medium':
            return 'bg-yellow-100 text-yellow-800 border-yellow-200';
        case 'low':
            return 'bg-green-100 text-green-800 border-green-200';
        default:
            return 'bg-gray-100 text-gray-800 border-gray-200';
    }
};

export const isTaskOverdue = (task: TaskI): boolean => {
    if (!task.dueDate || task.status === 'completed') {
        return false;
    }

    return new Date(task.dueDate) < new Date();
};

export const getStatusLabel = (status: string): string => {
    switch (status) {
        case 'open':
            return 'Open';
        case 'in-progress':
            return 'In Progress';
        case 'completed':
            return 'Completed';
        default:
            return status;
    }
};

export const getPriorityLabel = (priority: string): string => {
    return priority.charAt(0).toUpperCase() + priority.slice(1);
};

export const highlightText = (text: string, query: string): React.ReactNode => {
    if (!query.trim()) {
        return text;
    }

    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    const parts = text.split(regex);

    return parts.map((part, index) => {
        if (part.toLowerCase() === query.toLowerCase()) {
            return (
                <mark className="rounded bg-yellow-200 px-0.5 text-yellow-900" key={index}>
                    {part}
                </mark>
            );
        }

        return part;
    });
};

export const getHeaderText = (filters: {status: string; priority: string; assignee: string}): string => {
    const parts = [];

    if (filters.status !== 'all') {
        parts.push(filters.status.charAt(0).toUpperCase() + filters.status.slice(1).replace('-', ' '));
    }

    if (filters.priority !== 'all') {
        parts.push(filters.priority.charAt(0).toUpperCase() + filters.priority.slice(1) + ' priority');
    }

    if (filters.assignee !== 'all') {
        parts.push(`by ${filters.assignee}`);
    }

    if (parts.length === 0) {
        return 'All tasks';
    }

    return `${parts.join(', ')} tasks`;
};

export const sortTasks = (tasks: TaskI[], sortBy: SortOptionType, sortDirection: SortDirectionType): TaskI[] => {
    return [...tasks].sort((taskA, taskB) => {
        let valueA: string | number;
        let valueB: string | number;

        switch (sortBy) {
            case 'title':
                valueA = taskA.title.toLowerCase();
                valueB = taskB.title.toLowerCase();
                break;
            case 'priority':
                valueA = PRIORITY_ORDER[taskA.priority];
                valueB = PRIORITY_ORDER[taskB.priority];
                break;
            case 'status':
                valueA = STATUS_ORDER[taskA.status];
                valueB = STATUS_ORDER[taskB.status];
                break;
            case 'assignee':
                valueA = taskA.assignee.toLowerCase();
                valueB = taskB.assignee.toLowerCase();
                break;
            case 'dueDate':
                valueA = taskA.dueDate ? new Date(taskA.dueDate).getTime() : Number.MAX_SAFE_INTEGER;
                valueB = taskB.dueDate ? new Date(taskB.dueDate).getTime() : Number.MAX_SAFE_INTEGER;
                break;
            case 'created':
            default:
                valueA = new Date(taskA.createdAt).getTime();
                valueB = new Date(taskB.createdAt).getTime();
                break;
        }

        if (sortDirection === 'asc') {
            return valueA < valueB ? -1 : valueA > valueB ? 1 : 0;
        } else {
            return valueA > valueB ? -1 : valueA < valueB ? 1 : 0;
        }
    });
};
