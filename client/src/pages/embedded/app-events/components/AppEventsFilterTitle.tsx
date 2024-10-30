import {Badge} from '@/components/ui/badge';
import {Workflow} from '@/shared/middleware/embedded/configuration';

const AppEventsFilterTitle = ({
    filterData,
    workflows,
}: {
    filterData: {workflowId?: string};
    workflows: Workflow[] | undefined;
}) => {
    const label = workflows?.find((workflow) => workflow.id === filterData.workflowId)?.label;

    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">Filter by workflow:</span>

            <Badge variant="secondary">
                <span className="text-sm">{label ?? 'All Workflows'}</span>
            </Badge>
        </div>
    );
};

export default AppEventsFilterTitle;
