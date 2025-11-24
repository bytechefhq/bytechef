import Badge from '@/components/Badge/Badge';
import {Type} from '@/pages/automation/connections/Connections';
import {Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ConnectionsFilterTitle = ({
    componentDefinitions,
    filterData,
    tags,
}: {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    filterData: {id?: number | string | null | undefined; type: Type};
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Component) {
        pageTitle = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterData.id
        )?.title;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'component'}:
            </span>

            <Badge styleType="secondary-filled" weight="semibold">
                <span className="text-sm">{pageTitle ?? 'All Components'}</span>
            </Badge>
        </div>
    );
};

export default ConnectionsFilterTitle;
