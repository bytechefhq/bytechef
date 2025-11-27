import Badge from '@/components/Badge/Badge';
import {Type} from '@/ee/pages/embedded/connections/Connections';
import {Tag} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ConnectionsFilterTitle = ({
    componentDefinitions,
    filterData,
    tags,
}: {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    filterData: {id: string | number | null | undefined; type: Type};
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Component) {
        pageTitle = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterData.id
        )?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'component'}:
            </span>

            <Badge
                label={typeof pageTitle === 'string' ? pageTitle : 'All Components'}
                styleType="secondary-filled"
                weight="semibold"
            />
        </div>
    );
};

export default ConnectionsFilterTitle;
