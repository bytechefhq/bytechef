export type StatusFilterType = 'all' | 'open' | 'in-progress' | 'completed';
export type PriorityFilterType = 'all' | 'high' | 'medium' | 'low';
export type AssigneeFilterType = 'all' | string;
export type SortOptionType = 'created' | 'title' | 'priority' | 'status' | 'assignee' | 'dueDate';
export type SortDirectionType = 'asc' | 'desc';

export interface ApprovalTaskCommentI {
    id: string;
    author: string;
    content: string;
    timestamp: string;
}

export interface ApprovalTaskAttachmentI {
    id: string;
    name: string;
    size: string;
    type: string;
    uploadedBy: string;
    uploadedAt: string;
}

export interface ApprovalTaskI {
    assignee: string;
    attachments: ApprovalTaskAttachmentI[];
    comments: ApprovalTaskCommentI[];
    createdAt: string;
    dependencies: string[];
    description: string;
    dueDate?: string;
    id?: string;
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

export interface ApprovalTaskCountsI {
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

export interface ApprovalTaskTemplateI {
    id: string;
    name: string;
    description: string;
    defaultStatus: 'open' | 'in-progress' | 'completed';
    defaultPriority: 'high' | 'medium' | 'low';
    defaultAssignee?: string;
    estimatedDuration?: string;
    checklist: string[];
}

export interface NewApprovalTaskFormI {
    title: string;
    description: string;
    status: 'open' | 'in-progress' | 'completed';
    priority: 'high' | 'medium' | 'low';
    assignee: string;
    dueDate: string;
    dependencies: string[];
    templateId?: string;
}
