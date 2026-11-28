import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useState} from 'react';

interface CreateAutomationWithAiDialogProps {
    isPending: boolean;
    onClose: () => void;
    onCreate: (prompt: string) => void;
}

const CreateAutomationWithAiDialog = ({isPending, onClose, onCreate}: CreateAutomationWithAiDialogProps) => {
    const [prompt, setPrompt] = useState('');

    const trimmedPrompt = prompt.trim();

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-lg" showCloseButton>
                <DialogHeader>
                    <DialogTitle>Create with AI</DialogTitle>

                    <DialogDescription>
                        Describe what the automation should do. The AI Copilot builds it, and you can keep refining it
                        in the chat.
                    </DialogDescription>
                </DialogHeader>

                <fieldset className="flex flex-col gap-1.5 border-0">
                    <Label htmlFor="create-automation-with-ai-prompt">What should it do?</Label>

                    <Textarea
                        id="create-automation-with-ai-prompt"
                        onChange={(event) => setPrompt(event.target.value)}
                        placeholder="When a new row is added to my sheet, post it to Slack"
                        rows={5}
                        value={prompt}
                    />
                </fieldset>

                <DialogFooter>
                    <Button disabled={isPending} label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={isPending || !trimmedPrompt}
                        label="Create"
                        onClick={() => onCreate(trimmedPrompt)}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateAutomationWithAiDialog;
