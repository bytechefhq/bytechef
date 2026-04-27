import Badge from '@/components/Badge/Badge';
import DatePicker from '@/components/DatePicker/DatePicker';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import ApprovalForm from '@/shared/components/approval-form/ApprovalForm';
import {CheckCircle2Icon, CircleIcon, ClockIcon, UserIcon} from 'lucide-react';

import {useApprovalTaskDetail} from './hooks/useApprovalTaskDetail';

import type {ApprovalTaskI} from '../types/types';

export default function ApprovalTaskDetail() {
    const {
        approvalTask,
        availableAssigneeOptions,
        createdAtFormatted,
        handleAssigneeChange,
        handleDueDateChange,
        handlePriorityChange,
        handleStatusChange,
        priorityColor,
        statusIcon,
    } = useApprovalTaskDetail();

    const isCompleted = approvalTask?.status === 'completed';

    if (!approvalTask) {
        return (
            <div className="flex h-full flex-col items-center justify-center text-center">
                <CircleIcon className="mx-auto mb-4 size-12 text-muted-foreground" />

                <h2 className="text-lg font-medium text-foreground">Select an approval task</h2>

                <p className="text-sm text-muted-foreground">
                    Choose an approval task from the sidebar to view details
                </p>
            </div>
        );
    }

    return (
        <div className="h-full overflow-y-auto p-6">
            <div className="mb-6">
                <h1 className="text-xl font-semibold text-foreground">{approvalTask.title}</h1>
            </div>

            <div className="flex gap-6">
                <div className="min-w-0 max-w-lg flex-1">
                    <fieldset className="mb-6 border-0">
                        <span className="mb-2 block text-sm font-medium text-foreground">Description</span>

                        <p className="text-sm text-muted-foreground">
                            {approvalTask.description || 'No description provided'}
                        </p>
                    </fieldset>

                    {approvalTask.status === 'completed' ? (
                        <p className="text-lg font-medium text-foreground">This approval task has been completed.</p>
                    ) : approvalTask.jobResumeId ? (
                        <ApprovalForm
                            id={approvalTask.jobResumeId}
                            onSubmitted={() => handleStatusChange('completed')}
                            showHeader={false}
                        />
                    ) : (
                        <p className="text-sm text-muted-foreground">No approval form available for this task.</p>
                    )}
                </div>

                <div className="ml-auto flex w-1/4 flex-col gap-4">
                    <fieldset className="border-0">
                        <Label className="mb-2 block text-sm font-medium text-foreground">Status</Label>

                        <Select
                            disabled={isCompleted}
                            onValueChange={(value) => handleStatusChange(value as ApprovalTaskI['status'])}
                            value={approvalTask.status}
                        >
                            <SelectTrigger>
                                <SelectValue>
                                    <div className="flex items-center gap-2">
                                        {statusIcon}

                                        <span className="capitalize">{approvalTask.status.replace('-', ' ')}</span>
                                    </div>
                                </SelectValue>
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="open">
                                    <div className="flex items-center gap-2">
                                        <CircleIcon className="size-4 text-gray-500" />
                                        Open
                                    </div>
                                </SelectItem>

                                <SelectItem value="in-progress">
                                    <div className="flex items-center gap-2">
                                        <ClockIcon className="size-4 text-blue-500" />
                                        In Progress
                                    </div>
                                </SelectItem>

                                <SelectItem value="completed">
                                    <div className="flex items-center gap-2">
                                        <CheckCircle2Icon className="size-4 text-green-500" />
                                        Completed
                                    </div>
                                </SelectItem>
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-2 block text-sm font-medium text-foreground">Priority</Label>

                        <Select
                            disabled={isCompleted}
                            onValueChange={(value) => handlePriorityChange(value as ApprovalTaskI['priority'])}
                            value={approvalTask.priority}
                        >
                            <SelectTrigger>
                                <SelectValue>
                                    <Badge
                                        className={priorityColor}
                                        label={approvalTask.priority}
                                        styleType="outline-outline"
                                    />
                                </SelectValue>
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="high">High</SelectItem>

                                <SelectItem value="medium">Medium</SelectItem>

                                <SelectItem value="low">Low</SelectItem>
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-2 block text-sm font-medium text-foreground">Assignee</Label>

                        <Select
                            disabled={isCompleted}
                            onValueChange={handleAssigneeChange}
                            value={approvalTask.assigneeId ?? ''}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Select an assignee">
                                    <div className="flex items-center gap-2">
                                        <UserIcon className="size-4" />

                                        {approvalTask.assignee}
                                    </div>
                                </SelectValue>
                            </SelectTrigger>

                            <SelectContent>
                                {availableAssigneeOptions.map((assigneeOption) => (
                                    <SelectItem key={assigneeOption.id} value={assigneeOption.id}>
                                        <div className="flex items-center gap-2">
                                            <UserIcon className="size-4" />

                                            {assigneeOption.name}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-2 block text-sm font-medium text-foreground">Due Date</Label>

                        {isCompleted ? (
                            <p className="text-sm text-muted-foreground">
                                {approvalTask.dueDate
                                    ? new Date(approvalTask.dueDate).toLocaleDateString('en-US', {
                                          day: 'numeric',
                                          month: 'short',
                                          year: 'numeric',
                                      })
                                    : 'No due date'}
                            </p>
                        ) : (
                            <DatePicker
                                onChange={handleDueDateChange}
                                value={approvalTask.dueDate ? new Date(approvalTask.dueDate) : undefined}
                            />
                        )}
                    </fieldset>

                    <div>
                        <span className="mb-2 block text-sm font-medium text-foreground">Created</span>

                        <p className="text-sm text-muted-foreground">{createdAtFormatted}</p>
                    </div>
                </div>
            </div>
        </div>
    );
}
