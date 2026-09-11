import Badge from '@/components/Badge/Badge';

const WorkflowExecutionsFilterTitle = ({
    filterData,
}: {
    filterData: {
        environment?: number;
        status?: string;
    };
}) => {
    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

            {filterData.status && (
                <Badge label={`Status: ${filterData.status}`} styleType="primary-outline" weight="semibold" />
            )}

            {!filterData.status && <span className="text-sm text-muted-foreground uppercase">none</span>}
        </div>
    );
};

export default WorkflowExecutionsFilterTitle;
