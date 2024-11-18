import {Badge} from '@/components/ui/badge';
import {Type} from '@/ee/pages/automation/api-platform/api-collections/ApiCollections';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ApiCollectionsFilterTitle = ({
    environment,
    filterData,
    projects,
    tags,
}: {
    environment?: number;
    filterData: {id?: number; type: Type};
    projects: Project[] | undefined;
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Project) {
        pageTitle = projects?.find((project) => project.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">Filter by environment:</span>

            <Badge variant="secondary">
                <span className="text-sm">
                    {environment === undefined ? 'All Environments' : environment === 1 ? 'Test' : 'Production'}
                </span>
            </Badge>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'project'}:
            </span>

            <Badge variant="secondary">
                <span className="text-sm">{pageTitle ?? 'All Projects'}</span>
            </Badge>
        </div>
    );
};

export default ApiCollectionsFilterTitle;
