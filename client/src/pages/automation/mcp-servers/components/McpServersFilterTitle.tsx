import {Badge} from '@/components/ui/badge';
import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import {Tag} from '@/shared/middleware/graphql';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const McpServersFilterTitle = ({
    filterData,
    tags,
}: {
    filterData: {id?: string; type: Type};
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Tag && filterData.id) {
        pageTitle = tags?.find((tag) => tag.id === filterData.id!)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            {searchParams.get('tagId') && (
                <>
                    <span className="text-sm uppercase text-muted-foreground">tag:</span>

                    <Badge variant="secondary">
                        <span className="text-sm">{pageTitle ?? 'Unknown Tag'}</span>
                    </Badge>
                </>
            )}

            {!searchParams.get('tagId') && <span className="text-sm uppercase text-muted-foreground">none</span>}
        </div>
    );
};

export default McpServersFilterTitle;
