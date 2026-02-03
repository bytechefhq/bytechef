import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {
    AlertTriangleIcon,
    CheckCircle2Icon,
    CircleIcon,
    ClockIcon,
    EditIcon,
    SaveIcon,
    UserIcon,
    XCircleIcon,
} from 'lucide-react';

import {isTaskOverdue} from '../utils/task-utils';
import TaskComments from './TaskComments';
import {useTaskDetail} from './hooks/useTaskDetail';

export default function TaskDetail() {
    const {
        availableAssignees,
        createdAtFormatted,
        displayTask,
        handleCancel,
        handleEdit,
        handleFieldChange,
        handleSave,
        isEditing,
        priorityColor,
        statusIcon,
        task,
    } = useTaskDetail();

    if (!task) {
        return (
            <div className="flex h-full items-center justify-center text-center">
                <CircleIcon className="mx-auto mb-4 size-12 text-muted-foreground" />

                <h2 className="text-lg font-medium text-foreground">Select a task</h2>

                <p className="text-sm text-muted-foreground">Choose a task from the sidebar to view details</p>
            </div>
        );
    }

    if (!displayTask) {
        return null;
    }

    return (
        <div className="h-full overflow-y-auto p-6">
            <div className="mb-6">
                <div className="mb-4 flex items-start justify-between gap-4">
                    <div className="flex min-w-0 flex-1 items-center gap-3">
                        {isEditing ? (
                            <Input
                                className="flex-1 text-lg font-semibold"
                                onChange={(event) => handleFieldChange('title', event.target.value)}
                                value={displayTask.title}
                            />
                        ) : (
                            <h1 className="text-xl font-semibold text-foreground">{displayTask.title}</h1>
                        )}
                    </div>

                    <div className="flex shrink-0 items-center gap-2">
                        {isEditing && (
                            <Select
                                onValueChange={(value) => handleFieldChange('priority', value)}
                                value={displayTask.priority}
                            >
                                <SelectTrigger className="w-32">
                                    <SelectValue />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="high">High</SelectItem>

                                    <SelectItem value="medium">Medium</SelectItem>

                                    <SelectItem value="low">Low</SelectItem>
                                </SelectContent>
                            </Select>
                        )}

                        {!isEditing && isTaskOverdue(displayTask) && (
                            <Badge
                                className="text-red-600"
                                icon={<AlertTriangleIcon className="size-3" />}
                                label="Overdue"
                                styleType="destructive-outline"
                            />
                        )}

                        {isEditing ? (
                            <>
                                <Button icon={<SaveIcon className="size-4" />} onClick={handleSave} size="sm">
                                    Save
                                </Button>

                                <Button
                                    icon={<XCircleIcon className="size-4" />}
                                    onClick={handleCancel}
                                    size="sm"
                                    variant="outline"
                                >
                                    Cancel
                                </Button>
                            </>
                        ) : (
                            <Button
                                icon={<EditIcon className="size-4" />}
                                onClick={handleEdit}
                                size="sm"
                                variant="outline"
                            >
                                Edit
                            </Button>
                        )}
                    </div>
                </div>
            </div>

            <fieldset className="mb-6 border-0">
                <span className="mb-2 block text-sm font-medium text-foreground">Description</span>

                {isEditing ? (
                    <Textarea
                        className="min-h-[100px]"
                        onChange={(event) => handleFieldChange('description', event.target.value)}
                        placeholder="Add a description..."
                        value={displayTask.description}
                    />
                ) : (
                    <p className="text-sm text-muted-foreground">
                        {displayTask.description || 'No description provided'}
                    </p>
                )}
            </fieldset>

            <div className="mb-6 grid w-8/12 grid-cols-2">
                <fieldset className="border-0">
                    <Label className="mb-2 block text-sm font-medium text-foreground">Status</Label>

                    {isEditing ? (
                        <Select
                            onValueChange={(value) => handleFieldChange('status', value)}
                            value={displayTask.status}
                        >
                            <SelectTrigger>
                                <SelectValue />
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
                    ) : (
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            {statusIcon}

                            <span className="capitalize">{displayTask.status.replace('-', ' ')}</span>
                        </div>
                    )}
                </fieldset>

                <div>
                    <span className="mb-2 block text-sm font-medium text-foreground">Created</span>

                    <p className="text-sm text-muted-foreground">{createdAtFormatted}</p>
                </div>

                <fieldset className="border-0">
                    <Label className="mb-2 block text-sm font-medium text-foreground">Assignee</Label>

                    {isEditing ? (
                        <Select
                            onValueChange={(value) => handleFieldChange('assignee', value)}
                            value={displayTask.assignee}
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {availableAssignees.map((assignee) => (
                                    <SelectItem key={assignee} value={assignee}>
                                        <div className="flex items-center gap-2">
                                            <UserIcon className="size-4" />

                                            {assignee}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    ) : (
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <UserIcon className="size-4" />

                            {displayTask.assignee}
                        </div>
                    )}
                </fieldset>

                {!isEditing && (
                    <div>
                        <span className="mb-2 block text-sm font-medium text-foreground">Priority</span>

                        <Badge className={priorityColor} label={displayTask.priority} styleType="outline-outline" />
                    </div>
                )}
            </div>

            <div className="mb-6">
                <TaskComments comments={displayTask.comments} />
            </div>
        </div>
    );
}
