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

export type SourceComponentOptionType = {
    name: string;
    title?: string;
};

type ContextStoresFilterLeftSidebarNavPropsType = {
    sourceComponents: SourceComponentOptionType[];
    sourceComponentsIsLoading?: boolean;
    tags: TagOptionType[];
    tagsIsLoading?: boolean;
    filterData: ContextStoresFilterDataType;
};

const ContextStoresFilterLeftSidebarNav = ({
    filterData,
    sourceComponents,
    sourceComponentsIsLoading = false,
    tags,
    tagsIsLoading = false,
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
                loading={sourceComponentsIsLoading}
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
                        <span className="px-2 text-xs">No defined tags.</span>
                    )
                }
                className="mb-0"
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );
};

export default ContextStoresFilterLeftSidebarNav;
