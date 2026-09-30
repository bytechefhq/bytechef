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
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined}>
                <DialogMain>
                    <DialogHeader title="Add File" />

                    <DialogBody>
                        <fieldset className="flex flex-col gap-1 border-0 p-0">
                            <Label htmlFor="add-skill-file-path">File Path</Label>

                            <Input
                                id="add-skill-file-path"
                                onChange={(event) => setPath(event.target.value)}
                                placeholder="e.g. scripts/extract.py"
                                value={path}
                            />

                            {isDuplicate && (
                                <p className="text-sm text-content-destructive">
                                    A file with this path already exists.
                                </p>
                            )}

                            {isSkillMd && !isDuplicate && (
                                <p className="text-sm text-content-destructive">
                                    SKILL.md already exists and cannot be re-added.
                                </p>
                            )}

                            {hasInvalidPath && (
                                <p className="text-sm text-content-destructive">
                                    Path must not be absolute or contain traversal sequences (..).
                                </p>
                            )}
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button disabled={!isValid} label="Add" onClick={() => onAdd(trimmedPath)} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillFileAddDialog;
