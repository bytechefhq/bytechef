import {Badge} from '@/components/ui/badge';
const WorkflowExecutionsFilterTitle = ({
    filterData,
}: {
    filterData: {
        environment: string;
        status?: string;
    };
}) => {
    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">Filter by environment:</span>

            <Badge variant="secondary">
                <span className="text-sm">{filterData.environment}</span>
            </Badge>

            {filterData.status && (
                <>
                    <span className="text-sm uppercase text-muted-foreground">status:</span>

                    <Badge variant="secondary">
                        <span className="text-sm">{filterData.status}</span>
                    </Badge>
                </>
            )}
        </div>
    );
};

export default WorkflowExecutionsFilterTitle;
