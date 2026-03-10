import Badge from '@/components/Badge/Badge';
import {Type} from '@/pages/automation/mcp-servers/McpServers';
import {Tag} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

interface McpServersFilterTitleProps {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    filterData: {id?: string; type: Type};
    tags: Tag[] | undefined;
    uniqueProjects: Array<{
        id: string;
        name: string;
    }>;
}

const McpServersFilterTitle = ({
    componentDefinitions,
    filterData,
    tags,
    uniqueProjects,
}: McpServersFilterTitleProps) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    const filterId = filterData.id;
    const filterType = filterData.type;

    if (filterId && filterType === Type.Component) {
        const matchedComponent = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterId
        );

        pageTitle = matchedComponent?.title;
    } else if (filterId && filterType === Type.Project) {
        const matchedProject = uniqueProjects.find((project) => project.id === filterId);

        pageTitle = matchedProject?.name;
    } else if (filterId && filterType === Type.Tag) {
        const matchedTag = tags?.find((tag) => tag.id === filterId);

        pageTitle = matchedTag?.name;
    }

    let filterLabel: string | undefined;

    if (searchParams.get('componentName')) {
        filterLabel = 'component';
    } else if (searchParams.get('projectId')) {
        filterLabel = 'project';
    } else if (searchParams.get('tagId')) {
        filterLabel = 'tag';
    }

    return (
        <div className="space-x-1">
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            {filterLabel ? (
                <>
                    <span className="text-sm uppercase text-muted-foreground">{filterLabel}:</span>

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
