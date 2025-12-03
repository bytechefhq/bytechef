import Badge from '@/components/Badge/Badge';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';

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

            <Badge label={label ?? 'All Workflows'} styleType="secondary-filled" weight="semibold" />
        </div>
    );
};

export default AppEventsFilterTitle;
