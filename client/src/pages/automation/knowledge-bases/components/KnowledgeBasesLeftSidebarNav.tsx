import useKnowledgeBasesLeftSidebarNav from '@/pages/automation/knowledge-bases/components/hooks/useKnowledgeBasesLeftSidebarNav';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {TagIcon} from 'lucide-react';

const KnowledgeBasesLeftSidebarNav = () => {
    const {hasData, isLoading, tagId, tags} = useKnowledgeBasesLeftSidebarNav();

    if (!hasData) {
        return <></>;
    }

    return (
        <LeftSidebarNav
            body={
                !isLoading && (
                    <>
                        {tags.length ? (
                            tags.map((item) => (
                                <LeftSidebarNavItem
                                    icon={<TagIcon className="mr-1 size-4" />}
                                    item={{
                                        current: String(item!.id) === tagId,
                                        id: item!.id,
                                        name: item!.name,
                                    }}
                                    key={item!.id}
                                    toLink={`?tagId=${item!.id}`}
                                />
                            ))
                        ) : (
                            <span className="px-3 text-xs">No defined tags.</span>
                        )}
                    </>
                )
            }
            title="Tags"
        />
    );
};

export default KnowledgeBasesLeftSidebarNav;
