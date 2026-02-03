export type StatusFilterType = 'all' | 'open' | 'in-progress' | 'completed';
export type PriorityFilterType = 'all' | 'high' | 'medium' | 'low';
export type AssigneeFilterType = 'all' | string;
export type SortOptionType = 'created' | 'title' | 'priority' | 'status' | 'assignee' | 'dueDate';
export type SortDirectionType = 'asc' | 'desc';

export interface TaskCommentI {
    id: string;
    author: string;
    content: string;
    timestamp: string;
}

export interface TaskAttachmentI {
    id: string;
    name: string;
    size: string;
    type: string;
    uploadedBy: string;
    uploadedAt: string;
}

export interface TaskI {
    assignee: string;
    attachments: TaskAttachmentI[];
    comments: TaskCommentI[];
    createdAt: string;
    dependencies: string[];
    description: string;
    dueDate?: string;
    id: string;
    priority: 'high' | 'medium' | 'low';
    status: 'open' | 'in-progress' | 'completed';
    title: string;
    version?: number;
}

export interface FiltersI {
    status: StatusFilterType;
    priority: PriorityFilterType;
    assignee: AssigneeFilterType;
}

export interface TaskCountsI {
    all: number;
    open: number;
    'in-progress': number;
    completed: number;
}

export interface PriorityCountsI {
    all: number;
    high: number;
    medium: number;
    low: number;
}

export interface TaskTemplateI {
    id: string;
    name: string;
    description: string;
    defaultStatus: 'open' | 'in-progress' | 'completed';
    defaultPriority: 'high' | 'medium' | 'low';
    defaultAssignee?: string;
    estimatedDuration?: string;
    checklist: string[];
}

export interface NewTaskFormI {
    title: string;
    description: string;
    status: 'open' | 'in-progress' | 'completed';
    priority: 'high' | 'medium' | 'low';
    assignee: string;
    dueDate: string;
    dependencies: string[];
    templateId?: string;
}
