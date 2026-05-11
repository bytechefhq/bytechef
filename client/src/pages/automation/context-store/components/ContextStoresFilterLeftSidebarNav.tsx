import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {TagIcon} from 'lucide-react';

export enum ContextStoresFilterType {
    SourceComponent,
    Tag,
}

export type ContextStoresFilterDataType = {
    type: ContextStoresFilterType;
    sourceComponentName?: string;
    tagId?: number;
};

export type TagOptionType = {
    id: number;
    name: string;
};

/**
 * Source component, enriched with the display title fetched from the component-definitions registry. The `name`
 * stays the canonical id (lowercase like {@code "airtable"}, used as the URL filter token); `title` is what the
 * sidebar renders (Title-cased like {@code "Airtable"}).
 */
export type SourceComponentOptionType = {
    name: string;
    title?: string;
};

type ContextStoresFilterLeftSidebarNavPropsType = {
    /** Source components present across all stores' sources in the current workspace+env. */
    sourceComponents: SourceComponentOptionType[];
    /** Workspace tags used across context stores. */
    tags: TagOptionType[];
    filterData: ContextStoresFilterDataType;
};

/**
 * Left sidebar on the Context Stores page. Two filter groups — Components, Tags — modelled on
 * {@code McpServersLeftSidebarNav}: plain Title-cased component names with no leading icon, single-pick within group,
 * active filter encoded in the URL via {@code ?sourceComponentName=…} or {@code ?tagId=…}.
 */
const ContextStoresFilterLeftSidebarNav = ({
    filterData,
    sourceComponents,
    tags,
}: ContextStoresFilterLeftSidebarNavPropsType) => {
    const noSourceComponentFilter =
        filterData.type === ContextStoresFilterType.SourceComponent && !filterData.sourceComponentName;

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current: noSourceComponentFilter,
                                name: 'Components',
                            }}
                            toLink=""
                        />

                        {sourceComponents.map((component) => (
                            <LeftSidebarNavItem
                                item={{
                                    current:
                                        filterData.type === ContextStoresFilterType.SourceComponent &&
                                        filterData.sourceComponentName === component.name,
                                    id: component.name,
                                    name: component.title ?? component.name,
                                }}
                                key={component.name}
                                toLink={`?sourceComponentName=${encodeURIComponent(component.name)}`}
                            />
                        ))}
                    </>
                }
                title="Components"
            />

            <LeftSidebarNav
                body={
                    tags.length ? (
                        tags.map((tag) => (
                            <LeftSidebarNavItem
                                icon={<TagIcon className="mr-1 size-4" />}
                                item={{
                                    current:
                                        filterData.type === ContextStoresFilterType.Tag && filterData.tagId === tag.id,
                                    id: tag.id,
                                    name: tag.name,
                                }}
                                key={tag.id}
                                toLink={`?tagId=${tag.id}`}
                            />
                        ))
                    ) : (
                        <span className="px-3 text-xs">No defined tags.</span>
                    )
                }
                className="mb-0"
                title="Tags"
            />
        </>
    );
};

export default ContextStoresFilterLeftSidebarNav;
