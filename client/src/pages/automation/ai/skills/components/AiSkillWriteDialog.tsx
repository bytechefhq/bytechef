import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useAiSkillWriteForm from '@/pages/automation/ai/skills/hooks/useAiSkillWriteForm';

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
            <DialogContent className="sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Add a Skill</DialogTitle>
                </DialogHeader>

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

                <DialogFooter>
                    <Button onClick={() => onOpenChange(false)} variant="outline">
                        Cancel
                    </Button>

                    <Button
                        disabled={!name.trim() || !instructions.trim() || createSkillFromInstructionsMutation.isPending}
                        onClick={handleCreateFromInstructions}
                    >
                        {createSkillFromInstructionsMutation.isPending ? 'Creating...' : 'Create'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillWriteDialog;
