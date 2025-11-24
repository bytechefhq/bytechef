import Badge from '@/components/Badge/Badge';
import {Type} from '@/ee/pages/automation/api-platform/api-collections/ApiCollections';
import {ProjectBasic} from '@/ee/shared/middleware/automation/api-platform';
import {Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ApiCollectionsFilterTitle = ({
    filterData,
    projects,
    tags,
}: {
    filterData: {id?: number; type: Type};
    projects: ProjectBasic[] | undefined;
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
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'project'}:
            </span>

            <Badge
                label={typeof pageTitle === 'string' ? pageTitle : 'All Projects'}
                styleType="secondary-filled"
                weight="semibold"
            />
        </div>
    );
};

export default ApiCollectionsFilterTitle;
