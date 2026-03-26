import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {InfoIcon} from 'lucide-react';
import {useState} from 'react';

interface CreateTestDialogProps {
    onClose: () => void;
    onCreate: (name: string, description?: string) => void;
}

const CreateTestDialog = ({onClose, onCreate}: CreateTestDialogProps) => {
    const [description, setDescription] = useState('');
    const [name, setName] = useState('');

    const handleCreate = () => {
        if (!name.trim()) {
            return;
        }

        onCreate(name.trim(), description.trim() || undefined);
        onClose();
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create Test</DialogTitle>
                </DialogHeader>

                <fieldset className="flex flex-col gap-4 border-0 p-0">
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="test-name">Name</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    A short, descriptive name for this test suite. Use it to group related scenarios
                                    that evaluate a specific capability or behavior of the agent.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Input
                            id="test-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Enter test name"
                            value={name}
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="test-description">Description (optional)</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    An optional description explaining the purpose of this test, what agent behavior it
                                    validates, or any context that helps team members understand its intent.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Textarea
                            id="test-description"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Describe what this test evaluates"
                            rows={3}
                            value={description}
                        />
                    </div>
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button disabled={!name.trim()} label="Create" onClick={handleCreate} />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateTestDialog;
