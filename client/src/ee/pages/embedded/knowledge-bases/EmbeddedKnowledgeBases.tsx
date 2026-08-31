import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import EmbeddedKnowledgeBaseList from '@/ee/pages/embedded/knowledge-bases/components/EmbeddedKnowledgeBaseList';
import OwnerSelect from '@/ee/pages/embedded/shared/components/OwnerSelect';
import useEmbeddedConnectedUsers from '@/ee/pages/embedded/shared/components/useEmbeddedConnectedUsers';
import useKnowledgeBases from '@/shared/components/knowledge-bases/components/hooks/useKnowledgeBases';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAssignEmbeddedKnowledgeBaseOwnerMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BookOpenIcon} from 'lucide-react';
import {useState} from 'react';

const EmbeddedKnowledgeBases = () => {
    const [ownerId, setOwnerId] = useState<number | undefined>(undefined);

    const {connectedUsers} = useEmbeddedConnectedUsers();

    const queryClient = useQueryClient();

    const {error, isLoading, knowledgeBases} = useKnowledgeBases({ownerId, type: 'EMBEDDED'});

    const assignOwnerMutation = useAssignEmbeddedKnowledgeBaseOwnerMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['EmbeddedKnowledgeBases']});
        },
    });

    const handleAssign = (knowledgeBaseId: string, newOwnerId: number | undefined) => {
        assignOwnerMutation.mutate({
            input: {
                knowledgeBaseId,
                ownerId: newOwnerId === undefined ? undefined : String(newOwnerId),
            },
        });
    };

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            <div className="flex items-center gap-1">
                                <OwnerSelect
                                    connectedUsers={connectedUsers}
                                    noOwnerLabel="All owners"
                                    onChange={setOwnerId}
                                    ownerId={ownerId}
                                />
                            </div>
                        }
                        title="Knowledge Bases"
                    />
                }
            >
                {knowledgeBases.length > 0 ? (
                    <EmbeddedKnowledgeBaseList
                        connectedUsers={connectedUsers}
                        knowledgeBases={knowledgeBases}
                        onAssign={handleAssign}
                    />
                ) : (
                    <EmptyList
                        icon={<BookOpenIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Knowledge bases you create appear here, where you can assign them to an account."
                        title="No Knowledge Bases"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default EmbeddedKnowledgeBases;
