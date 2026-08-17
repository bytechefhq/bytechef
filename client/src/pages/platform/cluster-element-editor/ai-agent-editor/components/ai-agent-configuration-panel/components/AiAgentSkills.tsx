import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {useAiSkillsQuery} from '@/shared/middleware/graphql';
import {PlusIcon, SparklesIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import useAiAgentSkills from './hooks/useAiAgentSkills';

export default function AiAgentSkills() {
    const [open, setOpen] = useState(false);
    const [selectedSkillId, setSelectedSkillId] = useState('');

    const {canEdit, skillIds, updateSkillIds} = useAiAgentSkills();

    const {data} = useAiSkillsQuery();

    const skills = data?.aiSkills ?? [];

    const attachedSkills = skillIds.map((skillId) => ({
        id: skillId,
        name: skills.find((skill) => Number(skill.id) === skillId)?.name ?? String(skillId),
    }));

    const availableSkills = skills.filter((skill) => !skillIds.includes(Number(skill.id)));

    const handleAddSkill = () => {
        if (!selectedSkillId) {
            return;
        }

        updateSkillIds([...skillIds, Number(selectedSkillId)]);

        setOpen(false);
        setSelectedSkillId('');
    };

    const handleRemoveSkill = (skillId: number) => {
        updateSkillIds(skillIds.filter((attachedSkillId) => attachedSkillId !== skillId));
    };

    return (
        <div className="space-y-2">
            <div className="mb-3 flex items-center justify-between">
                <h2>Skills</h2>

                {canEdit && (
                    <Popover onOpenChange={setOpen} open={open}>
                        <PopoverTrigger asChild>
                            <Button icon={<PlusIcon />} label="Add skill" size="sm" variant="outline" />
                        </PopoverTrigger>

                        {/* A popover rather than a dialog: attaching a skill is a single choice, and a modal with
                            header, description and footer was heavy for one select. */}

                        <PopoverContent align="end" className="w-80 space-y-3">
                            <div className="space-y-1">
                                <Label htmlFor="ai-agent-skill">Skill</Label>

                                <Select
                                    disabled={availableSkills.length === 0}
                                    onValueChange={setSelectedSkillId}
                                    value={selectedSkillId}
                                >
                                    <SelectTrigger id="ai-agent-skill">
                                        <SelectValue
                                            placeholder={
                                                availableSkills.length === 0
                                                    ? 'No skills left to add'
                                                    : 'Choose a skill…'
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
                                <Button disabled={!selectedSkillId} label="Add" onClick={handleAddSkill} size="sm" />
                            </div>
                        </PopoverContent>
                    </Popover>
                )}
            </div>

            {attachedSkills.length === 0 ? (
                <p className="text-xs text-muted-foreground">No skills added yet.</p>
            ) : (
                attachedSkills.map((skill) => (
                    <div className="flex items-center gap-2 rounded bg-muted/50 p-2" key={skill.id}>
                        <SparklesIcon className="size-5 flex-none text-gray-700" />

                        <span className="flex-1 truncate text-xs font-medium">{skill.name}</span>

                        <Button
                            aria-label={`Remove ${skill.name}`}
                            icon={<Trash2Icon />}
                            onClick={() => handleRemoveSkill(skill.id)}
                            size="iconSm"
                            variant="destructiveGhost"
                        />
                    </div>
                ))
            )}
        </div>
    );
}
