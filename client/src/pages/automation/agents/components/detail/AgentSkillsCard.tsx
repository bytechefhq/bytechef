import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {
    AiAgentElement,
    useAddAiAgentElementMutation,
    useAiSkillsQuery,
    useDeleteAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon, SparklesIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

interface AgentSkillsCardProps {
    agentId: string;
    elements: AiAgentElement[];
}

const AgentSkillsCard = ({agentId, elements}: AgentSkillsCardProps) => {
    const [open, setOpen] = useState(false);
    const [skillId, setSkillId] = useState('');

    const {data} = useAiSkillsQuery();

    const skills = data?.aiSkills ?? [];

    const skillElements = elements.filter((element) => element.kind === 'SKILL');

    // Only skills the agent has not already attached are offered: a SKILL element carries no
    // per-attachment configuration, so a second row for the same skill would be a pure duplicate.
    const availableSkills = skills.filter(
        (skill) => !skillElements.some((element) => element.referenceId === skill.id)
    );

    const queryClient = useQueryClient();

    const addAiAgentElementMutation = useAddAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to add the skill.'),
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            setOpen(false);
            setSkillId('');
        },
    });

    const deleteAiAgentElementMutation = useDeleteAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to remove the skill.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const handleSave = () => {
        if (!skillId) {
            return;
        }

        addAiAgentElementMutation.mutate({input: {agentId, kind: 'SKILL', referenceId: skillId}});
    };

    return (
        <AgentSection
            action={
                <Popover onOpenChange={setOpen} open={open}>
                    <PopoverTrigger asChild>
                        <Button icon={<PlusIcon />} label="Add skill" size="sm" variant="outline" />
                    </PopoverTrigger>

                    {/* A popover rather than a dialog: attaching a skill is a single choice, and a modal with
                        header, description and footer was heavy for one select. Matches the simple editor's
                        skills section. */}

                    <PopoverContent align="end" className="w-80 space-y-3">
                        <div className="space-y-1">
                            <Label htmlFor="agent-skill">Skill</Label>

                            <Select disabled={availableSkills.length === 0} onValueChange={setSkillId} value={skillId}>
                                <SelectTrigger id="agent-skill">
                                    <SelectValue
                                        placeholder={
                                            availableSkills.length === 0 ? 'No skills left to add' : 'Choose a skill…'
                                        }
                                    />
                                </SelectTrigger>

                                <SelectContent>
                                    {availableSkills.map((skill) => (
                                        <SelectItem key={skill.id} value={skill.id}>
                                            {skill.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="flex justify-end">
                            <Button
                                disabled={!skillId || addAiAgentElementMutation.isPending}
                                label="Add"
                                onClick={handleSave}
                                size="sm"
                            />
                        </div>
                    </PopoverContent>
                </Popover>
            }
            title="Skills"
        >
            {skillElements.length === 0 ? (
                <p className="text-sm text-muted-foreground">No skills added yet.</p>
            ) : (
                <ul className="space-y-2">
                    {skillElements.map((element) => {
                        const skill = skills.find((candidate) => candidate.id === element.referenceId);

                        return (
                            <li
                                className="flex items-center justify-between rounded-md border border-border/50 p-3"
                                key={element.id}
                            >
                                <div className="flex items-center gap-2">
                                    <SparklesIcon aria-hidden className="size-3.5 text-content-neutral-tertiary" />

                                    <span className="font-medium">{skill?.name ?? element.referenceId}</span>
                                </div>

                                <Button
                                    aria-label={`Delete ${skill?.name ?? 'skill'}`}
                                    icon={<Trash2Icon />}
                                    onClick={() => deleteAiAgentElementMutation.mutate({id: element.id})}
                                    size="iconSm"
                                    variant="destructiveGhost"
                                />
                            </li>
                        );
                    })}
                </ul>
            )}
        </AgentSection>
    );
};

export default AgentSkillsCard;
