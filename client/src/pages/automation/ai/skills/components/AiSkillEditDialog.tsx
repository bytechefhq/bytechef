import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Input} from '@/components/ui/input';
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
        <AlertDialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Edit Skill</AlertDialogTitle>
                </AlertDialogHeader>

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

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onClose}>Cancel</AlertDialogCancel>

                    <AlertDialogAction
                        disabled={!isValid || !hasChanges}
                        onClick={() => onSave(trimmedName, trimmedDescription || null)}
                    >
                        Save
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default AiSkillEditDialog;
