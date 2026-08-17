import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiAgentElement,
    useAddAiAgentElementMutation,
    useAiAgentsQuery,
    useDeleteAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BotIcon, PlusIcon, Trash2Icon, TriangleAlertIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

interface AgentSubAgentsCardProps {
    agentId: string;
    elements: AiAgentElement[];
}

const AgentSubAgentsCard = ({agentId, elements}: AgentSubAgentsCardProps) => {
    const [open, setOpen] = useState(false);
    const [subAgentId, setSubAgentId] = useState('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data} = useAiAgentsQuery({workspaceId: String(currentWorkspaceId)});

    const candidates = (data?.aiAgents ?? []).filter((agent) => agent != null && agent.id !== agentId);

    const subAgentElements = elements.filter((element) => element.kind === 'SUB_AGENT');

    // An attached agent drops out of the picker: a SUB_AGENT element carries no per-attachment
    // configuration, so a second row for the same agent would be a pure duplicate.
    const availableCandidates = candidates.filter(
        (candidate) => !subAgentElements.some((element) => element.referenceId === candidate.id)
    );

    const queryClient = useQueryClient();

    const addAgentElementMutation = useAddAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to add the sub-agent.'),
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            setOpen(false);
            setSubAgentId('');
        },
    });

    const deleteAgentElementMutation = useDeleteAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to remove the sub-agent.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const handleSave = () => {
        if (!subAgentId) {
            return;
        }

        addAgentElementMutation.mutate({input: {agentId, kind: 'SUB_AGENT', referenceId: subAgentId}});
    };

    return (
        <AgentSection
            action={
                <Popover onOpenChange={setOpen} open={open}>
                    <PopoverTrigger asChild>
                        <Button
                            disabled={candidates.length === 0}
                            icon={<PlusIcon />}
                            label="Add sub-agent"
                            size="sm"
                            variant="outline"
                        />
                    </PopoverTrigger>

                    {/* A popover rather than a dialog, matching the skills section: attaching a sub-agent is a
                        single choice, and a modal with header, description and footer was heavy for one select. */}

                    <PopoverContent align="end" className="w-80 space-y-3">
                        <div className="space-y-1">
                            <Label htmlFor="agent-sub-agent">Agent</Label>

                            <Select
                                disabled={availableCandidates.length === 0}
                                onValueChange={setSubAgentId}
                                value={subAgentId}
                            >
                                <SelectTrigger id="agent-sub-agent">
                                    <SelectValue
                                        placeholder={
                                            availableCandidates.length === 0
                                                ? 'No agents left to add'
                                                : 'Choose an agent…'
                                        }
                                    />
                                </SelectTrigger>

                                <SelectContent>
                                    {availableCandidates.map((candidate) => (
                                        <SelectItem key={candidate.id} value={candidate.id}>
                                            {candidate.title}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex justify-end">
                            <Button
                                disabled={!subAgentId || addAgentElementMutation.isPending}
                                label="Add"
                                onClick={handleSave}
                                size="sm"
                            />
                        </div>
                    </PopoverContent>
                </Popover>
            }
            title="Sub-agents"
        >
            {subAgentElements.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                    {candidates.length === 0
                        ? 'No other agents available in this workspace yet.'
                        : 'No sub-agents added yet.'}
                </p>
            ) : (
                <ul className="space-y-2">
                    {subAgentElements.map((element) => {
                        const candidate = candidates.find((agent) => agent.id === element.referenceId);
                        const candidateHasSubAgents = (candidate?.elements ?? []).some(
                            (subElement) => subElement?.kind === 'SUB_AGENT'
                        );

                        return (
                            <li className="rounded-md border border-border/50 p-3" key={element.id}>
                                <div className="flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                        <BotIcon aria-hidden className="size-3.5 text-content-neutral-tertiary" />

                                        <span className="font-medium">{candidate?.title ?? element.referenceId}</span>
                                    </div>

                                    <Button
                                        aria-label={`Delete ${candidate?.title ?? 'sub-agent'}`}
                                        icon={<Trash2Icon />}
                                        onClick={() => deleteAgentElementMutation.mutate({id: element.id})}
                                        size="iconSm"
                                        variant="destructiveGhost"
                                    />
                                </div>

                                {candidateHasSubAgents && (
                                    <p className="mt-2 flex items-center gap-1 text-xs text-content-warning">
                                        <TriangleAlertIcon aria-hidden className="size-3.5" />
                                        This agent has sub-agents of its own; they won&apos;t be callable when it runs
                                        as a sub-agent.
                                    </p>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </AgentSection>
    );
};

export default AgentSubAgentsCard;
