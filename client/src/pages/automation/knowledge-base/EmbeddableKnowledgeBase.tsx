import PageLoader from '@/components/PageLoader';
import KnowledgeBaseInfoCard from '@/pages/automation/knowledge-base/components/KnowledgeBaseInfoCard';
import KnowledgeBaseTabs from '@/pages/automation/knowledge-base/components/KnowledgeBaseTabs';
import {useKnowledgeBaseQuery} from '@/shared/middleware/graphql';

interface EmbeddableKnowledgeBasePropsI {
    knowledgeBaseId: string;
}

const EmbeddableKnowledgeBase = ({knowledgeBaseId}: EmbeddableKnowledgeBasePropsI) => {
    const {data, error, isLoading} = useKnowledgeBaseQuery({id: knowledgeBaseId}, {enabled: !!knowledgeBaseId});

    const rawKnowledgeBase = data?.knowledgeBase ?? undefined;

    const documents = (rawKnowledgeBase?.documents ?? []).filter(
        (document): document is NonNullable<typeof document> => document !== null
    );

    const knowledgeBase = rawKnowledgeBase ? {...rawKnowledgeBase, documents} : undefined;

    return (
        <div className="flex size-full flex-col overflow-auto">
            <PageLoader errors={[error]} loading={isLoading}>
                {knowledgeBase && (
                    <div className="mx-auto flex h-full w-full max-w-(--breakpoint-lg) flex-col p-4">
                        <KnowledgeBaseInfoCard knowledgeBase={knowledgeBase} />

                        <KnowledgeBaseTabs documents={documents} knowledgeBaseId={knowledgeBaseId} />
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

export default EmbeddableKnowledgeBase;
