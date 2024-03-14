import {twMerge} from 'tailwind-merge';

const WorkflowExecutionBadge = ({success}: {success: boolean}) => {
    return (
        <div className="flex items-center">
            <div
                className={twMerge(
                    'flex h-[18px] w-14 items-center justify-center rounded-sm text-[10px] font-semibold uppercase',
                    success && 'bg-success text-success-foreground',
                    !success && 'bg-destructive text-destructive-foreground'
                )}
            >
                <span>{success ? 'Success' : 'Failure'}</span>
            </div>
        </div>
    );
};

export default WorkflowExecutionBadge;
