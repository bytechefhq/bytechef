import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useAgentSkillWriteForm from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAgentSkillWriteForm';

const AiAgentSkillWriteForm = () => {
    const {
        createSkillFromInstructionsMutation,
        description,
        handleCreateFromInstructions,
        instructions,
        name,
        setDescription,
        setInstructions,
        setName,
    } = useAgentSkillWriteForm();

    return (
        <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col py-6">
            <fieldset className="border-0 p-0">
                <div className="mb-4 flex flex-col gap-1.5">
                    <Label htmlFor="skill-name">Name</Label>

                    <Input
                        id="skill-name"
                        onChange={(event) => setName(event.target.value)}
                        placeholder="Enter skill name"
                        value={name}
                    />
                </div>

                <div className="mb-4 flex flex-col gap-1.5">
                    <Label htmlFor="skill-description">Description</Label>

                    <Input
                        id="skill-description"
                        onChange={(event) => setDescription(event.target.value)}
                        placeholder="Enter skill description"
                        value={description}
                    />
                </div>

                <div className="mb-6 flex flex-col gap-1.5">
                    <Label htmlFor="skill-instructions">Instructions</Label>

                    <Textarea
                        className="min-h-[200px]"
                        id="skill-instructions"
                        onChange={(event) => setInstructions(event.target.value)}
                        placeholder="Enter skill instructions..."
                        value={instructions}
                    />
                </div>

                <Button
                    disabled={!name.trim() || !instructions.trim() || createSkillFromInstructionsMutation.isPending}
                    onClick={handleCreateFromInstructions}
                >
                    {createSkillFromInstructionsMutation.isPending ? 'Creating...' : 'Create'}
                </Button>
            </fieldset>
        </div>
    );
};

export default AiAgentSkillWriteForm;
