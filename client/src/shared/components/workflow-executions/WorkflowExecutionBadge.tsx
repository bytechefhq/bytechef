import {Badge} from '@/components/ui/badge';

const WorkflowExecutionBadge = ({status}: {status: string}) => {
    return (
        <div className="flex items-center">
            <Badge
                className="uppercase"
                variant={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'destructive' : 'secondary'}
            >
                {status ?? ''}
            </Badge>
        </div>
    );
};

export default WorkflowExecutionBadge;
