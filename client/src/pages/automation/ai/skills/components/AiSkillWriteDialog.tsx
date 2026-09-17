import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useAiSkillWriteForm from '@/pages/automation/ai/skills/hooks/useAiSkillWriteForm';
import {PenLineIcon} from 'lucide-react';

interface AiSkillWriteDialogProps {
    onCreated?: (createdSkillId: string) => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const AiSkillWriteDialog = ({onCreated, onOpenChange, open}: AiSkillWriteDialogProps) => {
    const {
        createSkillFromInstructionsMutation,
        description,
        handleCreateFromInstructions,
        instructions,
        name,
        setDescription,
        setInstructions,
        setName,
    } = useAiSkillWriteForm({
        onSuccess: (createdSkillId) => {
            onCreated?.(createdSkillId);
            onOpenChange(false);
        },
    });

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent>
                <DialogMain className="sm:w-[672px] sm:max-w-full">
                    <DialogHeader
                        description="Enter a name, description and instructions for the skill."
                        icon={<PenLineIcon />}
                        title="Add a Skill"
                    />

                    <DialogBody>
                        <div className="flex flex-col gap-4">
                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="skill-name">Name</Label>

                                <Input
                                    id="skill-name"
                                    onChange={(event) => setName(event.target.value)}
                                    placeholder="Enter skill name"
                                    value={name}
                                />
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="skill-description">Description</Label>

                                <Input
                                    id="skill-description"
                                    onChange={(event) => setDescription(event.target.value)}
                                    placeholder="Enter skill description"
                                    value={description}
                                />
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="skill-instructions">Instructions</Label>

                                <Textarea
                                    className="max-h-[400px] min-h-[200px] overflow-y-auto"
                                    id="skill-instructions"
                                    onChange={(event) => setInstructions(event.target.value)}
                                    placeholder="Enter skill instructions..."
                                    value={instructions}
                                />
                            </div>
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={
                                !name.trim() || !instructions.trim() || createSkillFromInstructionsMutation.isPending
                            }
                            label={createSkillFromInstructionsMutation.isPending ? 'Creating...' : 'Create'}
                            onClick={handleCreateFromInstructions}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillWriteDialog;
