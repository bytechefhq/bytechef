import Badge from '@/components/Badge/Badge';
import {Type} from '@/pages/automation/mcp-servers/McpServers';
import {Tag} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

interface UniqueProjectI {
    id: string;
    name: string;
}

const McpServersFilterTitle = ({
    componentDefinitions,
    filterData,
    tags,
    uniqueProjects,
}: {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    filterData: {id?: string; type: Type};
    tags: Tag[] | undefined;
    uniqueProjects: UniqueProjectI[];
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Component && filterData.id) {
        pageTitle = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterData.id
        )?.title;
    } else if (filterData.type === Type.Project && filterData.id) {
        pageTitle = uniqueProjects.find((project) => project.id === filterData.id)?.name;
    } else if (filterData.type === Type.Tag && filterData.id) {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    const filterType = searchParams.get('componentName')
        ? 'component'
        : searchParams.get('projectId')
          ? 'project'
          : searchParams.get('tagId')
            ? 'tag'
            : undefined;

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            {filterType ? (
                <>
                    <span className="text-sm uppercase text-muted-foreground">{filterType}:</span>

                    <Badge
                        label={typeof pageTitle === 'string' ? pageTitle : 'Unknown'}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                </>
            ) : (
                <span className="text-sm uppercase text-muted-foreground">none</span>
            )}
        </div>
    );
};

export default McpServersFilterTitle;
