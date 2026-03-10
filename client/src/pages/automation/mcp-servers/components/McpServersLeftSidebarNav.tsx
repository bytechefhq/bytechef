import {Type} from '@/pages/automation/mcp-servers/McpServers';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Tag} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {TagIcon} from 'lucide-react';

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
                            componentDefinitions
                                ?.filter((componentDefinition: ComponentDefinitionBasic) =>
                                    allComponentNames.includes(componentDefinition.name)
                                )
                                ?.map((item: ComponentDefinitionBasic) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: filterData?.id === item.name && filterData.type === Type.Component,
                                            id: item.name!,
                                            name: item.title!,
                                        }}
                                        key={item.name}
                                        toLink={`?componentName=${item.name}`}
                                    />
                                ))}
                    </>
                }
                title="Components"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!mcpProjectsIsLoading &&
                            (uniqueProjects.length > 0 ? (
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
                                ))
                            ) : (
                                <span className="px-3 text-xs">No projects.</span>
                            ))}
                    </>
                }
                title="Projects"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!tagsIsLoading &&
                            (tags?.length ? (
                                tags.map((item) => (
                                    <LeftSidebarNavItem
                                        icon={<TagIcon className="mr-1 size-4" />}
                                        item={{
                                            current: filterData?.id === item!.id && filterData.type === Type.Tag,
                                            id: item!.id,
                                            name: item!.name,
                                        }}
                                        key={item!.id}
                                        toLink={`?tagId=${item!.id}`}
                                    />
                                ))
                            ) : (
                                <span className="px-3 text-xs">No defined tags.</span>
                            ))}
                    </>
                }
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
