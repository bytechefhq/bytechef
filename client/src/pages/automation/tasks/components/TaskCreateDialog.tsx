import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import DatePicker from '@/components/DatePicker/DatePicker';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {CheckCircle2, Circle, Clock, User} from 'lucide-react';
import {ReactNode} from 'react';

import {useTaskCreateDialog} from './hooks/useTaskCreateDialog';

interface TaskCreateDialogProps {
    trigger?: ReactNode;
}

export default function TaskCreateDialog({trigger}: TaskCreateDialogProps) {
    const {
        availableAssignees,
        errors,
        form,
        handleCloseDialog,
        handleFormChange,
        handleOpenChange,
        handleSubmit,
        isOpen,
    } = useTaskCreateDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={isOpen}>
            <DialogTrigger asChild>{trigger ?? <Button label="New Task" />}</DialogTrigger>

            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-workflow-test-configuration-dialog-width">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Create New Task</DialogTitle>

                        <DialogDescription>
                            Add a new task to your project. Fill in the details below to get started.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="task-title">Title *</Label>

                        <Input
                            className={errors.title ? 'border-red-500' : ''}
                            id="task-title"
                            onChange={(event) => handleFormChange('title', event.target.value)}
                            placeholder="Enter task title"
                            value={form.title}
                        />

                        {errors.title && <p className="text-sm text-red-500">{errors.title}</p>}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="task-description">Description *</Label>

                        <Textarea
                            className={`min-h-[80px] ${errors.description ? 'border-red-500' : ''}`}
                            id="task-description"
                            onChange={(event) => handleFormChange('description', event.target.value)}
                            placeholder="Describe what needs to be done"
                            value={form.description}
                        />

                        {errors.description && <p className="text-sm text-red-500">{errors.description}</p>}
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="task-status">Status</Label>

                            <Select onValueChange={(value) => handleFormChange('status', value)} value={form.status}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a status" />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="open">
                                        <div className="flex items-center gap-2">
                                            <Circle className="size-3 text-gray-400" />
                                            Open
                                        </div>
                                    </SelectItem>

                                    <SelectItem value="in-progress">
                                        <div className="flex items-center gap-2">
                                            <Clock className="size-3 text-blue-600" />
                                            In Progress
                                        </div>
                                    </SelectItem>

                                    <SelectItem value="completed">
                                        <div className="flex items-center gap-2">
                                            <CheckCircle2 className="size-3 text-green-600" />
                                            Completed
                                        </div>
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="task-priority">Priority</Label>

                            <Select
                                onValueChange={(value) => handleFormChange('priority', value)}
                                value={form.priority}
                            >
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a priority" />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="high">
                                        <div className="flex items-center gap-2">
                                            <Badge
                                                className="border-red-200 bg-red-100 text-red-800"
                                                label="High"
                                                styleType="outline-outline"
                                            />
                                        </div>
                                    </SelectItem>

                                    <SelectItem value="medium">
                                        <div className="flex items-center gap-2">
                                            <Badge
                                                className="border-yellow-200 bg-yellow-100 text-yellow-800"
                                                label="Medium"
                                                styleType="outline-outline"
                                            />
                                        </div>
                                    </SelectItem>

                                    <SelectItem value="low">
                                        <div className="flex items-center gap-2">
                                            <Badge
                                                className="border-green-200 bg-green-100 text-green-800"
                                                label="Low"
                                                styleType="outline-outline"
                                            />
                                        </div>
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="task-assignee">Assignee *</Label>

                            <Select
                                onValueChange={(value) => handleFormChange('assignee', value)}
                                value={form.assignee}
                            >
                                <SelectTrigger className={errors.assignee ? 'border-red-500' : ''}>
                                    <SelectValue placeholder="Select an assignee" />
                                </SelectTrigger>

                                <SelectContent>
                                    {availableAssignees.map((assignee) => (
                                        <SelectItem key={assignee} value={assignee}>
                                            <div className="flex items-center gap-2">
                                                <User className="size-3 text-muted-foreground" />

                                                {assignee}
                                            </div>
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>

                            {errors.assignee && <p className="text-sm text-red-500">{errors.assignee}</p>}
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="task-due-date">Due Date</Label>

                            <DatePicker
                                onChange={(date) =>
                                    handleFormChange('dueDate', date ? date.toISOString().split('T')[0] : '')
                                }
                                value={form.dueDate ? new Date(form.dueDate) : undefined}
                            />
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={handleCloseDialog} variant="outline">
                        Cancel
                    </Button>

                    <Button onClick={handleSubmit}>Create Task</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
