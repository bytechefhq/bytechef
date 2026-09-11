import Badge from '@/components/Badge/Badge';
import {Type} from '@/ee/pages/embedded/integrations/Integrations';
import {Category, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const IntegrationsFilterTitle = ({
    categories,
    filterData,
    tags,
}: {
    categories: Category[] | undefined;
    filterData: {id?: number | string; type: Type};
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
            <span className="text-sm text-muted-foreground uppercase">Filter by:</span>

            <Badge
                label={`${searchParams.get('tagId') ? 'Tags' : 'Categories'}: ${typeof pageTitle === 'string' ? pageTitle : 'All Categories'}`}
                styleType="primary-outline"
                weight="semibold"
            />
        </div>
    );
};

export default IntegrationsFilterTitle;
