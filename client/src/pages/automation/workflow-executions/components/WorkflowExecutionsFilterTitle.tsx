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
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            {filterData.status && (
                <>
                    <span className="text-sm uppercase text-muted-foreground">status:</span>

                    <Badge
                        label={filterData.status}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                </>
            )}

            {!filterData.status && <span className="text-sm uppercase text-muted-foreground">none</span>}
        </div>
    );
};

export default WorkflowExecutionsFilterTitle;
