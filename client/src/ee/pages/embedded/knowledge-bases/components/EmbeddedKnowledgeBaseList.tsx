import OwnerSelect from '@/ee/pages/embedded/shared/components/OwnerSelect';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

interface EmbeddedKnowledgeBaseI {
    description?: string | null;
    id: string;
    name: string;
    ownerId?: string | null;
}

interface EmbeddedKnowledgeBaseListProps {
    connectedUsers: ConnectedUser[];
    knowledgeBases: EmbeddedKnowledgeBaseI[];
    onAssign: (knowledgeBaseId: string, ownerId: number | undefined) => void;
}

const EmbeddedKnowledgeBaseList = ({connectedUsers, knowledgeBases, onAssign}: EmbeddedKnowledgeBaseListProps) => (
    <ul className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
        {knowledgeBases.map((knowledgeBase) => (
            <li className="flex items-center justify-between gap-4 py-4" key={knowledgeBase.id}>
                <div className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-semibold">{knowledgeBase.name}</span>

                    {knowledgeBase.description && (
                        <span className="mt-1 block truncate text-xs text-content-neutral-secondary">
                            {knowledgeBase.description}
                        </span>
                    )}
                </div>

                <OwnerSelect
                    connectedUsers={connectedUsers}
                    noOwnerLabel="Shared"
                    onChange={(ownerId) => onAssign(knowledgeBase.id, ownerId)}
                    ownerId={knowledgeBase.ownerId == null ? undefined : Number(knowledgeBase.ownerId)}
                />
            </li>
        ))}
    </ul>
);

export default EmbeddedKnowledgeBaseList;
