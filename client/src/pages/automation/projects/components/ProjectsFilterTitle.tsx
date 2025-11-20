import Badge from '@/components/Badge/Badge';
import {Type} from '@/pages/automation/projects/Projects';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ProjectsFilterTitle = ({
    categories,
    filterData,
    tags,
}: {
    categories: Category[] | undefined;
    filterData: {id?: number; type: Type};
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">{`Filter by ${searchParams.get('tagId') ? 'tag' : 'category'}:`}</span>

            <Badge label={`${pageTitle ?? 'All Categories'}`} styleType="secondary-filled" weight="semibold" />
        </div>
    );
};

export default ProjectsFilterTitle;
