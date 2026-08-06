import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Tag, useAiSkillTagsQuery} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';
import {useLocation, useSearchParams} from 'react-router-dom';

/**
 * The Tags filter section of the skills sidebars (list and detail view) — the data-tables idiom:
 * one nav item per tag linking to `?tagId=`, plus an All Tags item clearing the filter.
 */
const AiSkillsTagsNav = () => {
    const location = useLocation();

    const [searchParams] = useSearchParams();
    const tagId = searchParams.get('tagId');

    const {data: tagsData} = useAiSkillTagsQuery();

    const tags = (tagsData?.aiSkillTags ?? []) as Tag[];

    return (
        <LeftSidebarNav
            body={
                tags.length ? (
                    tags.map((tag) => (
                        <LeftSidebarNavItem
                            icon={<TagIcon className="mr-1 size-4" />}
                            item={{
                                current: String(tag.id) === tagId,
                                id: tag.id,
                                // Clicking the active tag clears the filter — the tag sidebars carry no
                                // explicit "all" entry (see DataTablesLeftSidebarNav).
                                name: tag.name,
                            }}
                            key={tag.id}
                            toLink={String(tag.id) === tagId ? location.pathname : `?tagId=${tag.id}`}
                        />
                    ))
                ) : (
                    <span className="px-3 text-xs">No defined tags.</span>
                )
            }
            title="Tags"
        />
    );
};

export default AiSkillsTagsNav;
