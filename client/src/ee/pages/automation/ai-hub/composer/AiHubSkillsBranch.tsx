import {CommandEmpty, CommandGroup, CommandItem} from '@/components/ui/command';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {useAiSkillsQuery} from '@/shared/middleware/graphql';
import {ChevronLeftIcon, HexagonIcon} from 'lucide-react';

interface AiHubSkillsBranchPropsI {
    onBack: () => void;
    onClose: () => void;
}

const AiHubSkillsBranch = ({onBack, onClose}: AiHubSkillsBranchPropsI) => {
    const {data} = useAiSkillsQuery();

    const skills = data?.aiSkills ?? [];

    const handleSelect = (id: string, name: string) => {
        aiHubComposerStore.getState().addSkill({id, name});

        onClose();
    };

    return (
        <>
            <CommandGroup>
                <CommandItem onSelect={onBack} value="back-to-root">
                    <ChevronLeftIcon className="mr-2 size-3.5" />

                    <span className="flex-1 text-muted-foreground">Back</span>
                </CommandItem>
            </CommandGroup>

            <CommandGroup heading="Skills">
                {skills.length === 0 && <CommandEmpty>No skills.</CommandEmpty>}

                {skills.map((skill) => (
                    <CommandItem
                        key={`skill-${skill.id}`}
                        onSelect={() => handleSelect(skill.id, skill.name)}
                        value={`skill-${skill.id}-${skill.name}`}
                    >
                        <HexagonIcon className="mr-2 size-3.5 shrink-0 text-muted-foreground" />

                        <div className="flex min-w-0 flex-col">
                            <span className="truncate">{skill.name}</span>

                            {skill.description && (
                                <span className="truncate text-xs text-muted-foreground">{skill.description}</span>
                            )}
                        </div>
                    </CommandItem>
                ))}
            </CommandGroup>
        </>
    );
};

export default AiHubSkillsBranch;
