import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useState} from 'react';

interface AiSkillEditDescriptionDialogProps {
    currentDescription?: string | null;
    onClose: () => void;
    onSave: (description: string | null) => void;
}

const AiSkillEditDescriptionDialog = ({currentDescription, onClose, onSave}: AiSkillEditDescriptionDialogProps) => {
    const [description, setDescription] = useState(currentDescription ?? '');

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
                    <AlertDialogTitle>Edit Description</AlertDialogTitle>
                </AlertDialogHeader>

                <div className="py-2">
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

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onClose}>Cancel</AlertDialogCancel>

                    <AlertDialogAction
                        disabled={description.trim() === (currentDescription ?? '')}
                        onClick={() => onSave(description.trim() || null)}
                    >
                        Save
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default AiSkillEditDescriptionDialog;
