import Badge from '@/components/Badge/Badge';

const WorkflowExecutionBadge = ({status}: {status: string}) => {
    const getStyleType = () => {
        if (status === 'COMPLETED') {
            return 'success-outline';
        }

        if (status === 'FAILED') {
            return 'destructive-filled';
        }

        return 'secondary-filled';
    };

    return (
        <div className="flex items-center">
            <Badge className="uppercase" label={status ?? ''} styleType={getStyleType()} />
        </div>
    );
};

export default WorkflowExecutionBadge;
