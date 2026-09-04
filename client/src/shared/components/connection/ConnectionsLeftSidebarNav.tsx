import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {TagIcon} from 'lucide-react';
import {useMemo} from 'react';

interface ConnectionsLeftSidebarNavTagI {
    id?: number;
    name: string;
}

interface ConnectionsLeftSidebarNavProps {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    /**
     * The connections the component list is drawn from — the unfiltered set where a filter is active, so
     * narrowing to one component does not empty the rail that offers the other components.
     */
    connections: {componentName: string}[] | undefined;
    connectionsAreLoading?: boolean;
    currentComponentName?: string;
    currentTagId?: number;
    tags: ConnectionsLeftSidebarNavTagI[] | undefined;
    tagsIsLoading?: boolean;
}

/**
 * The connections rail, shared by the automation and embedded Connections pages. Both filter the same way — by
 * the component a connection belongs to, or by tag — so the rail lives here and each page passes its own
 * query results in.
 */
const ConnectionsLeftSidebarNav = ({
    componentDefinitions,
    connections,
    connectionsAreLoading = false,
    currentComponentName,
    currentTagId,
    tags,
    tagsIsLoading = false,
}: ConnectionsLeftSidebarNavProps) => {
    const componentItems = useMemo(() => {
        const connectedComponentNames = new Set((connections ?? []).map((connection) => connection.componentName));

        return (componentDefinitions ?? [])
            .filter((componentDefinition) => connectedComponentNames.has(componentDefinition.name))
            .map((componentDefinition) => ({
                current: currentComponentName === componentDefinition.name,
                id: componentDefinition.name,
                name: componentDefinition.title!,
                toLink: `?componentName=${componentDefinition.name}`,
            }));
    }, [componentDefinitions, connections, currentComponentName]);

    return (
        <>
            <LeftSidebarFilterNav
                items={componentItems}
                leadItem={{
                    current: !currentComponentName && currentTagId === undefined,
                    name: 'All Components',
                }}
                loading={connectionsAreLoading}
                title="Components"
            />

            <LeftSidebarFilterNav
                emptyMessage="No tags."
                icon={<TagIcon className="mr-1 size-4" />}
                items={(tags ?? []).map((tag) => ({
                    current: currentTagId === tag.id,
                    id: tag.id!,
                    name: tag.name,
                    toLink: `?tagId=${tag.id}`,
                }))}
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );
};

export default ConnectionsLeftSidebarNav;
