import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useState} from 'react';

interface AiSkillEditDialogProps {
    currentDescription?: string | null;
    currentName: string;
    onClose: () => void;
    onSave: (name: string, description: string | null) => void;
}

const AiSkillEditDialog = ({currentDescription, currentName, onClose, onSave}: AiSkillEditDialogProps) => {
    const [description, setDescription] = useState(currentDescription ?? '');
    const [name, setName] = useState(currentName);

    const trimmedName = name.trim();
    const trimmedDescription = description.trim();

    const hasChanges = trimmedName !== currentName || trimmedDescription !== (currentDescription ?? '');
    const isValid = trimmedName.length > 0;

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <DialogTitle>Edit Skill</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="flex flex-col gap-4 py-2">
                    <div>
                        <Label htmlFor="edit-skill-name">Name</Label>

                        <Input
                            className="mt-1"
                            id="edit-skill-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Enter skill name"
                            value={name}
                        />
                    </div>

                    <div>
                        <Label htmlFor="edit-skill-description">Description</Label>

                        <Textarea
                            className="mt-1 resize-none"
                            id="edit-skill-description"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Enter skill description"
                            rows={4}
                            value={description}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={onClose} variant="outline">
                        Cancel
                    </Button>

                    <Button
                        disabled={!isValid || !hasChanges}
                        onClick={() => onSave(trimmedName, trimmedDescription || null)}
                    >
                        Save
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillEditDialog;
