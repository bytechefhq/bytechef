import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiAgentElement,
    useAddAiAgentElementMutation,
    useDeleteAiAgentElementMutation,
    useKnowledgeBasesQuery,
    useUpdateAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

interface AgentKnowledgeBaseCardProps {
    agentId: string;
    elements: AiAgentElement[];
}

/**
 * Lists the agent's knowledge bases, each with its own retrieval parameters.
 *
 * RAG is a multiple cluster element, so an agent carries one rag entry per knowledge base and Top K / similarity
 * threshold belong to each entry rather than to the agent — two knowledge bases of different sizes rarely want
 * the same retrieval depth.
 */
const AgentKnowledgeBaseCard = ({agentId, elements}: AgentKnowledgeBaseCardProps) => {
    const [open, setOpen] = useState(false);
    const [knowledgeBaseId, setKnowledgeBaseId] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data} = useKnowledgeBasesQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const knowledgeBases = (data?.knowledgeBases ?? []).filter((knowledgeBase) => knowledgeBase != null);

    const knowledgeBaseElements = elements.filter((element) => element.kind === 'KNOWLEDGE_BASE');

    const attachedIds = new Set(knowledgeBaseElements.map((element) => element.referenceId));

    const availableKnowledgeBases = knowledgeBases.filter((knowledgeBase) => !attachedIds.has(knowledgeBase.id));

    const queryClient = useQueryClient();

    const onError = (error: unknown) => {
        toast.error(error instanceof Error ? error.message : 'Failed to save the knowledge base.');
    };

    const addAiAgentElementMutation = useAddAiAgentElementMutation({
        onError,
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            setOpen(false);
            setKnowledgeBaseId('');
        },
    });

    const deleteAiAgentElementMutation = useDeleteAiAgentElementMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const updateAiAgentElementMutation = useUpdateAiAgentElementMutation({
        onError,
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const handleAdd = () => {
        if (!knowledgeBaseId) {
            return;
        }

        addAiAgentElementMutation.mutate({input: {agentId, kind: 'KNOWLEDGE_BASE', referenceId: knowledgeBaseId}});
    };

    // updateAiAgentElement replaces a row's ENTIRE parameters map, so the sibling field is sent back with every
    // edit rather than patched.
    const handleNumberBlur = (element: AiAgentElement, field: 'similarityThreshold' | 'topK', value: string) => {
        const parameters = {...(element.parameters ?? {})} as Record<string, unknown>;

        const trimmed = value.trim();

        if (!trimmed) {
            delete parameters[field];
        } else {
            const parsed = Number(trimmed);

            if (!Number.isFinite(parsed)) {
                return;
            }

            parameters[field] = parsed;
        }

        updateAiAgentElementMutation.mutate({input: {id: element.id, parameters}});
    };

    const knowledgeBaseName = (referenceId?: string | null) =>
        knowledgeBases.find((knowledgeBase) => knowledgeBase.id === referenceId)?.name ?? 'Unknown knowledge base';

    return (
        <AgentSection
            action={
                <Popover onOpenChange={setOpen} open={open}>
                    <PopoverTrigger asChild>
                        <Button
                            disabled={availableKnowledgeBases.length === 0}
                            icon={<PlusIcon />}
                            label="Add knowledge base"
                            size="sm"
                            variant="outline"
                        />
                    </PopoverTrigger>

                    {/* A popover rather than a dialog, matching the skills and sub-agent sections: attaching a
                        knowledge base is a single choice. */}

                    <PopoverContent align="end" className="w-80 space-y-3">
                        <div className="space-y-1">
                            <Label htmlFor="agent-knowledge-base">Knowledge base</Label>

                            <Select onValueChange={setKnowledgeBaseId} value={knowledgeBaseId}>
                                <SelectTrigger id="agent-knowledge-base">
                                    <SelectValue placeholder="Choose a knowledge base…" />
                                </SelectTrigger>

                                <SelectContent>
                                    {availableKnowledgeBases.map((knowledgeBase) => (
                                        <SelectItem key={knowledgeBase.id} value={knowledgeBase.id}>
                                            {knowledgeBase.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex justify-end">
                            <Button
                                disabled={!knowledgeBaseId || addAiAgentElementMutation.isPending}
                                label="Add"
                                onClick={handleAdd}
                                size="sm"
                            />
                        </div>
                    </PopoverContent>
                </Popover>
            }
            title="Knowledge base"
        >
            {knowledgeBaseElements.length === 0 ? (
                <p className="text-sm text-muted-foreground">No knowledge bases added yet.</p>
            ) : (
                <ul className="space-y-2">
                    {knowledgeBaseElements.map((element) => (
                        <li
                            aria-label={knowledgeBaseName(element.referenceId)}
                            className="rounded-md border border-border/50 p-3"
                            key={element.id}
                        >
                            <div className="flex items-center justify-between gap-2">
                                <span className="truncate font-medium">{knowledgeBaseName(element.referenceId)}</span>

                                <Button
                                    aria-label={`Remove ${knowledgeBaseName(element.referenceId)}`}
                                    icon={<Trash2Icon />}
                                    onClick={() => deleteAiAgentElementMutation.mutate({id: element.id})}
                                    size="iconSm"
                                    variant="destructiveGhost"
                                />
                            </div>

                            <fieldset className="mt-2 grid grid-cols-1 gap-2 border-0 p-0 sm:grid-cols-2">
                                <div className="space-y-1">
                                    <Label htmlFor={`agent-knowledge-base-${element.id}-top-k`}>Top K</Label>

                                    <Input
                                        defaultValue={
                                            element.parameters?.topK != null ? String(element.parameters.topK) : ''
                                        }
                                        id={`agent-knowledge-base-${element.id}-top-k`}
                                        onBlur={(event) => handleNumberBlur(element, 'topK', event.target.value)}
                                        placeholder="e.g. 5"
                                        type="number"
                                    />
                                </div>

                                <div className="space-y-1">
                                    <Label htmlFor={`agent-knowledge-base-${element.id}-similarity-threshold`}>
                                        Similarity threshold
                                    </Label>

                                    <Input
                                        defaultValue={
                                            element.parameters?.similarityThreshold != null
                                                ? String(element.parameters.similarityThreshold)
                                                : ''
                                        }
                                        id={`agent-knowledge-base-${element.id}-similarity-threshold`}
                                        onBlur={(event) =>
                                            handleNumberBlur(element, 'similarityThreshold', event.target.value)
                                        }
                                        placeholder="e.g. 0.7"
                                        step="0.01"
                                        type="number"
                                    />
                                </div>
                            </fieldset>
                        </li>
                    ))}
                </ul>
            )}
        </AgentSection>
    );
};

export default AgentKnowledgeBaseCard;
