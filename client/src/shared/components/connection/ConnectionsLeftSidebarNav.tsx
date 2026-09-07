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
    connections: {componentName: string}[] | undefined;
    connectionsAreLoading?: boolean;
    currentComponentName?: string;
    currentTagId?: number;
    tags: ConnectionsLeftSidebarNavTagI[] | undefined;
    tagsIsLoading?: boolean;
}

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
