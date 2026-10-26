import Badge from '@/components/Badge/Badge';
import {
    ContextStoresFilterDataType,
    ContextStoresFilterType,
    SourceComponentOptionType,
    TagOptionType,
} from '@/pages/automation/context-store/components/ContextStoresFilterLeftSidebarNav';
import {useSearchParams} from 'react-router-dom';

type ContextStoresFilterTitlePropsType = {
    filterData: ContextStoresFilterDataType;
    sourceComponents: SourceComponentOptionType[];
    tags: TagOptionType[];
};

const ContextStoresFilterTitle = ({filterData, sourceComponents, tags}: ContextStoresFilterTitlePropsType) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | undefined;

    if (filterData.type === ContextStoresFilterType.SourceComponent) {
        if (filterData.sourceComponentName) {
            const match = sourceComponents.find((component) => component.name === filterData.sourceComponentName);

            pageTitle = match?.title ?? filterData.sourceComponentName;
        }
    } else {
        pageTitle = tags.find((tag) => tag.id === filterData.tagId)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm text-muted-foreground uppercase">
                {`Filter by ${searchParams.get('tagId') ? 'tag' : 'component'}:`}
            </span>

            <Badge label={pageTitle ?? 'Components'} styleType="secondary-filled" weight="semibold" />
        </div>
    );
};

export default ContextStoresFilterTitle;
