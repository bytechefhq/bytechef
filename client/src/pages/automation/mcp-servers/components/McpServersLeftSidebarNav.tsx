import {Type} from '@/pages/automation/mcp-servers/McpServers';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
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
    tags,
    tagsIsLoading,
    uniqueProjects,
}: McpServersLeftSidebarNavProps) => {
    const filteredComponentDefinitions = useMemo(
        () =>
            componentDefinitions?.filter((componentDefinition) =>
                allComponentNames.includes(componentDefinition.name)
            ) ?? [],
        [componentDefinitions, allComponentNames]
    );

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current: !filterData?.id && filterData.type === Type.Component,
                                name: 'All Components',
                            }}
                            toLink=""
                        />

                        {!componentDefinitionsIsLoading &&
                            filteredComponentDefinitions.map((componentDefinition: ComponentDefinitionBasic) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current:
                                            filterData?.id === componentDefinition.name &&
                                            filterData.type === Type.Component,
                                        id: componentDefinition.name!,
                                        name: componentDefinition.title!,
                                    }}
                                    key={componentDefinition.name}
                                    toLink={`?componentName=${componentDefinition.name}`}
                                />
                            ))}
                    </>
                }
                title="Components"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!mcpProjectsIsLoading && uniqueProjects.length === 0 && (
                            <span className="px-3 text-xs">No projects.</span>
                        )}

                        {!mcpProjectsIsLoading &&
                            uniqueProjects.map((project) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current: filterData?.id === project.id && filterData.type === Type.Project,
                                        id: project.id!,
                                        name: project.name,
                                    }}
                                    key={project.id}
                                    toLink={`?projectId=${project.id}`}
                                />
                            ))}
                    </>
                }
                title="Projects"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!tagsIsLoading && !tags?.length && <span className="px-3 text-xs">No defined tags.</span>}

                        {!tagsIsLoading &&
                            tags?.map((tag) => (
                                <LeftSidebarNavItem
                                    icon={<TagIcon className="mr-1 size-4" />}
                                    item={{
                                        current: filterData?.id === tag!.id && filterData.type === Type.Tag,
                                        id: tag!.id,
                                        name: tag!.name,
                                    }}
                                    key={tag!.id}
                                    toLink={`?tagId=${tag!.id}`}
                                />
                            ))}
                    </>
                }
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
