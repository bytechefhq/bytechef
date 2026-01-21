import useDataTablesLeftSidebarNav from '@/pages/automation/datatables/components/hooks/useDataTablesLeftSidebarNav';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {TagIcon} from 'lucide-react';

const DataTablesLeftSidebarNav = () => {
    const {isLoading, tagId, tags} = useDataTablesLeftSidebarNav();

    if (isLoading || tags.length === 0) {
        return <></>;
    }

    return (
        <LeftSidebarNav
            body={
                <>
                    {tags.length ? (
                        tags.map((tag) => (
                            <LeftSidebarNavItem
                                icon={<TagIcon className="mr-1 size-4" />}
                                item={{
                                    current: String(tag.id) === tagId,
                                    id: tag.id,
                                    name: tag.name,
                                }}
                                key={tag.id}
                                toLink={`?tagId=${tag.id}`}
                            />
                        ))
                    ) : (
                        <span className="px-3 text-xs">No defined tags.</span>
                    )}
                </>
            }
            title="Tags"
        />
    );
};

export default DataTablesLeftSidebarNav;
