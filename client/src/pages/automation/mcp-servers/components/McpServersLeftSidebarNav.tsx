import {Type} from '@/pages/automation/mcp-servers/McpServers';
import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {Tag} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {TagIcon} from 'lucide-react';
import {useMemo} from 'react';

interface McpServersLeftSidebarNavProps {
    allComponentNames: string[];
    componentDefinitions?: ComponentDefinitionBasic[];
    componentDefinitionsIsLoading: boolean;
    filterData: {id?: string; type: Type};
    mcpProjectsIsLoading: boolean;
    mcpServersIsLoading?: boolean;
    tags?: Tag[];
    tagsIsLoading: boolean;
    uniqueProjects: {id: string; name: string}[];
}

const McpServersLeftSidebarNav = ({
    allComponentNames,
    componentDefinitions,
    componentDefinitionsIsLoading,
    filterData,
    mcpProjectsIsLoading,
    mcpServersIsLoading = false,
    tags,
    tagsIsLoading,
    uniqueProjects,
}: McpServersLeftSidebarNavProps) => {
    const componentItems = useMemo(
        () =>
            (componentDefinitions ?? [])
                .filter((componentDefinition) => allComponentNames.includes(componentDefinition.name))
                .map((componentDefinition: ComponentDefinitionBasic) => ({
                    current: filterData?.id === componentDefinition.name && filterData.type === Type.Component,
                    id: componentDefinition.name,
                    name: componentDefinition.title!,
                    toLink: `?componentName=${componentDefinition.name}`,
                })),
        [allComponentNames, componentDefinitions, filterData]
    );

    return (
        <>
            <LeftSidebarFilterNav
                items={componentItems}
                leadItem={{
                    current: !filterData?.id && filterData.type === Type.Component,
                    name: 'All Components',
                }}
                loading={componentDefinitionsIsLoading || mcpServersIsLoading}
                title="Components"
            />

            <LeftSidebarFilterNav
                emptyMessage="No projects."
                items={uniqueProjects.map((project) => ({
                    current: filterData?.id === project.id && filterData.type === Type.Project,
                    id: project.id,
                    name: project.name,
                    toLink: `?projectId=${project.id}`,
                }))}
                loading={mcpProjectsIsLoading}
                title="Projects"
            />

            <LeftSidebarFilterNav
                emptyMessage="No defined tags."
                icon={<TagIcon className="mr-1 size-4" />}
                items={(tags ?? []).map((tag) => ({
                    current: filterData?.id === tag.id && filterData.type === Type.Tag,
                    id: tag.id,
                    name: tag.name,
                    toLink: `?tagId=${tag.id}`,
                }))}
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
