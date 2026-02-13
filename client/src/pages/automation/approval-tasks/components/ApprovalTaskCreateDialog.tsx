import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import DatePicker from '@/components/DatePicker/DatePicker';
import RequiredMark from '@/components/RequiredMark';
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
import {CheckCircle2Icon, CircleIcon, ClockIcon, UserIcon} from 'lucide-react';
import {ReactNode} from 'react';

import {useApprovalTaskCreateDialog} from './hooks/useApprovalTaskCreateDialog';

interface ApprovalTaskCreateDialogProps {
    trigger?: ReactNode;
}

export default function ApprovalTaskCreateDialog({trigger}: ApprovalTaskCreateDialogProps) {
    const {
        availableAssignees,
        errors,
        form,
        handleCloseDialog,
        handleFormChange,
        handleOpenChange,
        handleSubmit,
        isOpen,
    } = useApprovalTaskCreateDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={isOpen}>
            <DialogTrigger asChild>{trigger ?? <Button label="New Approval Task" />}</DialogTrigger>

            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-workflow-test-configuration-dialog-width">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Create New Approval Task</DialogTitle>

                        <DialogDescription>
                            Add a new approval task to your project. Fill in the details below to get started.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <fieldset className="space-y-2 border-0">
                        <Label htmlFor="approval-task-title">
                            Title
                            <RequiredMark />
                        </Label>

                        <Input
                            className={errors.title ? 'border-red-500' : ''}
                            id="approval-task-title"
                            onChange={(event) => handleFormChange('title', event.target.value)}
                            placeholder="Enter approval task title"
                            value={form.title}
                        />

                        {errors.title && <p className="text-sm text-red-500">{errors.title}</p>}
                    </fieldset>

                    <fieldset className="space-y-2 border-0">
                        <Label htmlFor="approval-task-description">
                            Description
                            <RequiredMark />
                        </Label>

                        <Textarea
                            className={`min-h-[80px] ${errors.description ? 'border-red-500' : ''}`}
                            id="approval-task-description"
                            onChange={(event) => handleFormChange('description', event.target.value)}
                            placeholder="Describe what needs to be done"
                            value={form.description}
                        />

                        {errors.description && <p className="text-sm text-red-500">{errors.description}</p>}
                    </fieldset>

                    <div className="grid grid-cols-2 gap-4">
                        <fieldset className="space-y-2 border-0">
                            <Label>Status</Label>

                            <Select onValueChange={(value) => handleFormChange('status', value)} value={form.status}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a status" />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="open">
                                        <span className="flex items-center gap-2">
                                            <CircleIcon className="size-3 text-gray-400" />
                                            Open
                                        </span>
                                    </SelectItem>

                                    <SelectItem value="in-progress">
                                        <span className="flex items-center gap-2">
                                            <ClockIcon className="size-3 text-blue-600" />
                                            In Progress
                                        </span>
                                    </SelectItem>

                                    <SelectItem value="completed">
                                        <span className="flex items-center gap-2">
                                            <CheckCircle2Icon className="size-3 text-green-600" />
                                            Completed
                                        </span>
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </fieldset>

                        <fieldset className="space-y-2 border-0">
                            <Label>Priority</Label>

                            <Select
                                onValueChange={(value) => handleFormChange('priority', value)}
                                value={form.priority}
                            >
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a priority" />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="high">
                                        <Badge
                                            className="border-red-200 bg-red-100 text-red-800"
                                            label="High"
                                            styleType="outline-outline"
                                        />
                                    </SelectItem>

                                    <SelectItem value="medium">
                                        <Badge
                                            className="border-yellow-200 bg-yellow-100 text-yellow-800"
                                            label="Medium"
                                            styleType="outline-outline"
                                        />
                                    </SelectItem>

                                    <SelectItem value="low">
                                        <Badge
                                            className="border-green-200 bg-green-100 text-green-800"
                                            label="Low"
                                            styleType="outline-outline"
                                        />
                                    </SelectItem>
                                </SelectContent>
                            </Select>
                        </fieldset>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <fieldset className="space-y-2 border-0">
                            <Label>
                                Assignee
                                <RequiredMark />
                            </Label>

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
                                            <span className="flex items-center gap-2">
                                                <UserIcon className="size-3 text-muted-foreground" />

                                                {assignee}
                                            </span>
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>

                            {errors.assignee && <p className="text-sm text-red-500">{errors.assignee}</p>}
                        </fieldset>

                        <fieldset className="space-y-2 border-0">
                            <Label>Due Date</Label>

                            <DatePicker
                                onChange={(date) =>
                                    handleFormChange('dueDate', date ? date.toISOString().split('T')[0] : '')
                                }
                                value={form.dueDate ? new Date(form.dueDate) : undefined}
                            />
                        </fieldset>
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={handleCloseDialog} variant="outline">
                        Cancel
                    </Button>

                    <Button onClick={handleSubmit}>Create Approval Task</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
