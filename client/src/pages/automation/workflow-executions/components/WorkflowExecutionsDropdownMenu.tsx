import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {useToast} from '@/hooks/use-toast';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useStopJobMutation} from '@/shared/mutations/platform/jobs.mutations';
import {CellContext} from '@tanstack/react-table';
import {CircleStopIcon, EllipsisVerticalIcon, ViewIcon} from 'lucide-react';
import {useState} from 'react';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

/* eslint-disable-next-line @typescript-eslint/no-explicit-any */
const WorkflowExecutionsDropdownMenu = ({data}: {data: CellContext<WorkflowExecution, any>}) => {
    const [disabled, setDisabled] = useState(data.getValue()?.status !== 'STARTED');
    const {toast} = useToast();

    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore();

    const stopJobMutation = useStopJobMutation({
        onError: () =>
            toast({
                className: 'mt-2 w-[340px] rounded-md bg-red-600 p-4 text-white',
                description: 'Failed to stop Workflow Execution',
            }),
        onSuccess: () =>
            toast({
                className: 'mt-2 w-[340px] rounded-md bg-green-600 p-4 text-white',
                description: 'Stopping Workflow Execution',
            }),
    });

    const handleViewClick = () => {
        const id = data.row.original.id;

        if (id != null) {
            setWorkflowExecutionId(id);
            setWorkflowExecutionSheetOpen(true);
        }
    };

    const handleStopWorkflowExecutionClick = (id: number) => {
        stopJobMutation.mutate(id);
        setDisabled(true);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="center" onClick={(e) => e.stopPropagation()}>
                <DropdownMenuItem className="dropdown-menu-item" onClick={handleViewClick}>
                    <ViewIcon /> View
                </DropdownMenuItem>

                <DropdownMenuItem
                    className="dropdown-menu-item-destructive"
                    disabled={disabled}
                    onClick={() => handleStopWorkflowExecutionClick(data.getValue()?.id)}
                >
                    <CircleStopIcon /> Stop
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default WorkflowExecutionsDropdownMenu;
