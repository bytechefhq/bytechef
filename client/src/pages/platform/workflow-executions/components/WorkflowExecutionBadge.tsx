import {Badge} from '@/components/ui/badge';

const WorkflowExecutionBadge = ({success}: {success: boolean}) => {
    return (
        <div className="flex items-center">
            <Badge
                className="flex h-[18px] w-14 items-center justify-center rounded-sm text-[10px] font-semibold uppercase"
                variant={success ? 'success' : 'destructive'}
            >
                <span>{success ? 'Success' : 'Failure'}</span>
            </Badge>
        </div>
    );
};

export default WorkflowExecutionBadge;
