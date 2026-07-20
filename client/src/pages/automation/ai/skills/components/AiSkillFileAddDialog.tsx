import {Input} from '@/components/Input/Input';
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
import {useState} from 'react';

interface AiSkillFileAddDialogProps {
    existingPaths: string[];
    onAdd: (path: string) => void;
    onClose: () => void;
}

const AiSkillFileAddDialog = ({existingPaths, onAdd, onClose}: AiSkillFileAddDialogProps) => {
    const [path, setPath] = useState('');

    const trimmedPath = path.trim();

    const isDuplicate = existingPaths.some((existingPath) => existingPath.toLowerCase() === trimmedPath.toLowerCase());
    const isSkillMd = trimmedPath.toLowerCase() === 'skill.md';
    const hasInvalidPath = trimmedPath.includes('..') || trimmedPath.startsWith('/');
    const isValid = trimmedPath.length > 0 && !isDuplicate && !isSkillMd && !hasInvalidPath;

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
                    <AlertDialogTitle>Add File</AlertDialogTitle>
                </AlertDialogHeader>

                <div className="flex flex-col gap-1 py-2">
                    <Label htmlFor="add-skill-file-path">File Path</Label>

                    <Input
                        id="add-skill-file-path"
                        onChange={(event) => setPath(event.target.value)}
                        placeholder="e.g. scripts/extract.py"
                        value={path}
                    />

                    {isDuplicate && (
                        <p className="text-sm text-red-600">A file with this path already exists.</p>
                    )}

                    {isSkillMd && !isDuplicate && (
                        <p className="text-sm text-red-600">SKILL.md already exists and cannot be re-added.</p>
                    )}

                    {hasInvalidPath && (
                        <p className="text-sm text-red-600">
                            Path must not be absolute or contain traversal sequences (..).
                        </p>
                    )}
                </div>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={() => onClose()}>Cancel</AlertDialogCancel>

                    <AlertDialogAction disabled={!isValid} onClick={() => onAdd(trimmedPath)}>
                        Add
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default AiSkillFileAddDialog;
