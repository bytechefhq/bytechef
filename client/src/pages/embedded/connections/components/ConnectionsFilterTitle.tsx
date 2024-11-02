import {Badge} from '@/components/ui/badge';
import {Type} from '@/pages/embedded/connections/Connections';
import {Tag} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const ConnectionsFilterTitle = ({
    componentDefinitions,
    environment,
    filterData,
    tags,
}: {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    environment?: number;
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
            <span className="text-sm uppercase text-muted-foreground">Filter by environment:</span>

            <Badge variant="secondary">
                <span className="text-sm">
                    {environment === undefined
                        ? 'All Environments'
                        : environment === 1
                          ? 'Development'
                          : environment === 2
                            ? 'Test'
                            : 'Production'}
                </span>
            </Badge>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'component'}:
            </span>

            <Badge variant="secondary">
                <span className="text-sm">{pageTitle ?? 'All Components'}</span>
            </Badge>
        </div>
    );
};

export default ConnectionsFilterTitle;
