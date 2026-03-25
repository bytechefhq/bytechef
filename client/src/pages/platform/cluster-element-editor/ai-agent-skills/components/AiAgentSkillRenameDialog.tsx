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
import {useState} from 'react';

interface AiAgentSkillRenameDialogProps {
    currentName: string;
    onClose: () => void;
    onRename: (newName: string) => void;
}

const AiAgentSkillRenameDialog = ({currentName, onClose, onRename}: AiAgentSkillRenameDialogProps) => {
    const [name, setName] = useState(currentName);

    return (
        <AlertDialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open={true}
        >
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Rename Skill</AlertDialogTitle>
                </AlertDialogHeader>

                <div className="py-2">
                    <Label htmlFor="rename-skill">Name</Label>

                    <Input
                        id="rename-skill"
                        onChange={(event) => setName(event.target.value)}
                        placeholder="Enter skill name"
                        value={name}
                    />
                </div>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onClose}>Cancel</AlertDialogCancel>

                    <AlertDialogAction
                        disabled={!name.trim() || name.trim() === currentName}
                        onClick={() => onRename(name.trim())}
                    >
                        Rename
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default AiAgentSkillRenameDialog;
