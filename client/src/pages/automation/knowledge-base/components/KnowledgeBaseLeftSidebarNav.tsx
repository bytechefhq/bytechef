import useKnowledgeBaseLeftSidebarNav from '@/pages/automation/knowledge-base/components/hooks/useKnowledgeBaseLeftSidebarNav';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {DatabaseIcon} from 'lucide-react';

const KnowledgeBaseLeftSidebarNav = () => {
    const {currentKnowledgeBaseId, isLoading, knowledgeBases} = useKnowledgeBaseLeftSidebarNav();

    return (
        <LeftSidebarNav
            body={
                !isLoading && (
                    <>
                        {knowledgeBases.length ? (
                            knowledgeBases.map((knowledgeBase) => (
                                <LeftSidebarNavItem
                                    icon={<DatabaseIcon className="mr-1 size-4" />}
                                    item={{
                                        current: knowledgeBase.id === currentKnowledgeBaseId,
                                        id: knowledgeBase.id,
                                        name: knowledgeBase.name,
                                    }}
                                    key={knowledgeBase.id}
                                    toLink={`/automation/knowledge-bases/${knowledgeBase.id}`}
                                />
                            ))
                        ) : (
                            <span className="px-3 text-xs">No knowledge bases.</span>
                        )}
                    </>
                )
            }
            title="Knowledge Bases"
        />
    );
};

export default KnowledgeBaseLeftSidebarNav;
