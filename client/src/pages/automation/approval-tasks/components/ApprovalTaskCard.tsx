import Badge from '@/components/Badge/Badge';
import {AlertTriangleIcon, CalendarIcon, LinkIcon, MessageSquareIcon, PaperclipIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {useApprovalTasksStore} from '../stores/useApprovalTasksStore';
import {isApprovalTaskOverdue} from '../utils/approval-task-utils';

import type {MouseEvent} from 'react';

import type {ApprovalTaskI} from '../types/types';

interface ApprovalTaskCardProps {
    isSelected: boolean;
    onSelect: () => void;
    onStatusToggle: (event: MouseEvent) => void;
    approvalTask: ApprovalTaskI;
}

export default function ApprovalTaskCard({approvalTask, isSelected, onSelect, onStatusToggle}: ApprovalTaskCardProps) {
    const {
        getFormattedDueDate,
        getHighlightedAssignee,
        getHighlightedDescription,
        getHighlightedTitle,
        getPriorityColor,
        getStatusIcon,
    } = useApprovalTasksStore();

    const highlightedTitle = getHighlightedTitle(approvalTask);
    const highlightedDescription = getHighlightedDescription(approvalTask);
    const highlightedAssignee = getHighlightedAssignee(approvalTask);
    const statusIcon = getStatusIcon(approvalTask);
    const priorityColor = getPriorityColor(approvalTask);
    const formattedDueDate = getFormattedDueDate(approvalTask);
    const isOverdue = isApprovalTaskOverdue(approvalTask);

    return (
        <div
            className={twMerge(
                'cursor-pointer rounded-lg border p-3 transition-colors hover:bg-muted/50',
                isSelected ? 'border-primary bg-muted' : 'border-border bg-background'
            )}
            onClick={onSelect}
        >
            <div className="flex w-full items-start gap-3">
                <div
                    className="mt-0.5 shrink-0 cursor-pointer rounded-full p-1 transition-colors hover:bg-muted"
                    onClick={onStatusToggle}
                    title={`Click to change status (currently: ${approvalTask.status.replace('-', ' ')})`}
                >
                    {statusIcon}
                </div>

                <div className="min-w-0 flex-1 overflow-hidden">
                    <div className="mb-1 flex flex-wrap items-center gap-2">
                        <h3 className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
                            {highlightedTitle}
                        </h3>

                        <div className="flex shrink-0 items-center gap-1">
                            <Badge
                                className={twMerge('text-xs', priorityColor)}
                                label={approvalTask.priority}
                                styleType="outline-outline"
                            />

                            {isOverdue && (
                                <Badge
                                    className="text-xs"
                                    icon={<AlertTriangleIcon className="mr-1 size-3" />}
                                    label="Overdue"
                                    styleType="destructive-filled"
                                />
                            )}
                        </div>
                    </div>

                    <p className="mb-2 line-clamp-2 break-words text-xs text-muted-foreground">
                        {highlightedDescription}
                    </p>

                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                        <span className="mr-2 min-w-0 flex-1 truncate">{highlightedAssignee}</span>

                        <div className="flex shrink-0 items-center gap-2">
                            {formattedDueDate && (
                                <div className="flex items-center gap-1">
                                    <CalendarIcon className="size-3" />

                                    <span>{formattedDueDate}</span>
                                </div>
                            )}

                            {approvalTask.comments.length > 0 && (
                                <div className="flex items-center gap-1">
                                    <MessageSquareIcon className="size-3" />

                                    <span>{approvalTask.comments.length}</span>
                                </div>
                            )}

                            {approvalTask.attachments.length > 0 && (
                                <div className="flex items-center gap-1">
                                    <PaperclipIcon className="size-3" />

                                    <span>{approvalTask.attachments.length}</span>
                                </div>
                            )}

                            {approvalTask.dependencies.length > 0 && (
                                <div className="flex items-center gap-1">
                                    <LinkIcon className="size-3" />

                                    <span>{approvalTask.dependencies.length}</span>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
