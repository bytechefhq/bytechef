import {Input} from '@/components/Input/Input';
import KnowledgeBaseDropdownMenu from '@/pages/automation/knowledge-base/components/KnowledgeBaseDropdownMenu';
import useKnowledgeBaseLeftSidebarNav from '@/pages/automation/knowledge-base/components/hooks/useKnowledgeBaseLeftSidebarNav';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {KnowledgeBase} from '@/shared/middleware/graphql';

/**
 * The sidebar's search box mirrors the data tables and context store sidebars: filtering happens in the hook, so
 * the list below is already the filtered one, and the box stands in for a group heading — the list is navigation,
 * and past a handful of knowledge bases a filter is worth more than a label. The empty message distinguishes the
 * two reasons it can be empty — a workspace with no knowledge bases at all reads differently from a search that
 * matched none.
 */
const KnowledgeBaseLeftSidebarNav = () => {
    const {currentKnowledgeBaseId, handleSearchChange, isLoading, knowledgeBases, search} =
        useKnowledgeBaseLeftSidebarNav();

    return (
        <div className="flex h-full flex-col">
            <div className="space-y-2 px-3 pt-0.5 pb-3">
                <Input
                    onChange={(event) => handleSearchChange(event.target.value)}
                    placeholder="Search knowledge bases..."
                    value={search ?? ''}
                />
            </div>

            <div className="flex-1 overflow-y-auto">
                <LeftSidebarNav
                    body={
                        !isLoading && (
                            <>
                                {knowledgeBases.length ? (
                                    knowledgeBases.map((knowledgeBase) => (
                                        <LeftSidebarNavItem
                                            item={{
                                                current: knowledgeBase.id === currentKnowledgeBaseId,
                                                id: knowledgeBase.id,
                                                name: knowledgeBase.name,
                                            }}
                                            key={knowledgeBase.id}
                                            toLink={`/automation/knowledge-bases/${knowledgeBase.id}`}
                                            trailing={
                                                <KnowledgeBaseDropdownMenu
                                                    className="size-6 opacity-0 transition-opacity group-hover:opacity-100 data-[state=open]:opacity-100"
                                                    knowledgeBase={knowledgeBase as KnowledgeBase}
                                                />
                                            }
                                        />
                                    ))
                                ) : (
                                    <span className="px-3 text-xs">
                                        {search ? 'No knowledge bases found.' : 'No knowledge bases.'}
                                    </span>
                                )}
                            </>
                        )
                    }
                />
            </div>
        </div>
    );
};

export default KnowledgeBaseLeftSidebarNav;
