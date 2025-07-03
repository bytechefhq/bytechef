import {Badge} from '@/components/ui/badge';
import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import {Tag} from '@/shared/middleware/graphql';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const McpServersFilterTitle = ({
    environment,
    filterData,
    tags,
}: {
    environment?: number;
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
            <span className="text-sm uppercase text-muted-foreground">Filter by environment:</span>

            <Badge variant="secondary">
                <span className="text-sm">
                    {environment === undefined
                        ? 'All Environments'
                        : environment === 1
                          ? 'Development'
                          : environment === 2
                            ? 'Staging'
                            : 'Production'}
                </span>
            </Badge>

            {searchParams.get('tagId') && (
                <>
                    <span className="text-sm uppercase text-muted-foreground">tag:</span>

                    <Badge variant="secondary">
                        <span className="text-sm">{pageTitle ?? 'Unknown Tag'}</span>
                    </Badge>
                </>
            )}
        </div>
    );
};

export default McpServersFilterTitle;
