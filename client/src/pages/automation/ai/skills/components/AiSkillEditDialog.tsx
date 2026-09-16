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
import {PencilIcon} from 'lucide-react';
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
                <DialogMain>
                    <DialogHeader
                        description="Update the skill name and description."
                        icon={<PencilIcon />}
                        title="Edit Skill"
                    />

                    <DialogBody>
                        <div className="flex flex-col gap-4">
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
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={!isValid || !hasChanges}
                            label="Save"
                            onClick={() => onSave(trimmedName, trimmedDescription || null)}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillEditDialog;
