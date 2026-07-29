import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Textarea} from '@/components/ui/textarea';
import {useGenerateAiSkillMutation} from '@/shared/middleware/graphql';
import {Loader2Icon, SparklesIcon} from 'lucide-react';
import {useState} from 'react';

interface AiSkillGenerateDialogProps {
    onCreated?: (createdSkillId: string) => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const AiSkillGenerateDialog = ({onCreated, onOpenChange, open}: AiSkillGenerateDialogProps) => {
    const [prompt, setPrompt] = useState('');

    const generateAiSkillMutation = useGenerateAiSkillMutation({
        onSuccess: (data) => {
            onCreated?.(data.generateAiSkill.id);
            onOpenChange(false);
        },
    });

    const {isPending} = generateAiSkillMutation;

    const handleGenerateClick = () => {
        const trimmedPrompt = prompt.trim();

        if (!trimmedPrompt) {
            return;
        }

        generateAiSkillMutation.mutate({prompt: trimmedPrompt});
    };

    return (
        <Dialog onOpenChange={(nextOpen) => !isPending && onOpenChange(nextOpen)} open={open}>
            <DialogContent className="sm:max-w-2xl" onInteractOutside={(event) => isPending && event.preventDefault()}>
                <DialogHeader>
                    <DialogTitle>Create Skill with AI</DialogTitle>

                    <DialogDescription>
                        Describe the skill you want and AI will build it for you. This can take a minute.
                    </DialogDescription>
                </DialogHeader>

                <Textarea
                    autoFocus
                    className="max-h-[400px] min-h-[160px] overflow-y-auto"
                    disabled={isPending}
                    onChange={(event) => setPrompt(event.target.value)}
                    placeholder="Summarize my unread Gmail and send me a daily digest."
                    value={prompt}
                />

                <DialogFooter>
                    <Button disabled={isPending} onClick={() => onOpenChange(false)} variant="outline">
                        Cancel
                    </Button>

                    <Button disabled={isPending || !prompt.trim()} onClick={handleGenerateClick}>
                        {isPending ? (
                            <>
                                <Loader2Icon className="mr-1 size-4 animate-spin" /> Generating…
                            </>
                        ) : (
                            <>
                                <SparklesIcon className="mr-1 size-4" /> Generate
                            </>
                        )}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiSkillGenerateDialog;
