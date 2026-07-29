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
import {useGenerateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {Loader2Icon, SparklesIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

interface GenerateWorkflowDialogProps {
    onClose: () => void;
    projectId: number;
}

const GenerateWorkflowDialog = ({onClose, projectId}: GenerateWorkflowDialogProps) => {
    const [prompt, setPrompt] = useState('');

    const navigate = useNavigate();

    const generateProjectWorkflowMutation = useGenerateProjectWorkflowMutation({
        onSuccess: (response) => {
            navigate(`/automation/projects/${projectId}/project-workflows/${response.projectWorkflowId}`);

            onClose();
        },
    });

    const {isPending, mutate} = generateProjectWorkflowMutation;

    const handleGenerateClick = () => {
        const trimmedPrompt = prompt.trim();

        if (!trimmedPrompt) {
            return;
        }

        mutate({id: projectId, prompt: trimmedPrompt});
    };

    return (
        <Dialog onOpenChange={(open) => !open && !isPending && onClose()} open>
            <DialogContent onInteractOutside={(event) => isPending && event.preventDefault()}>
                <DialogHeader>
                    <DialogTitle>Generate Workflow with AI</DialogTitle>

                    <DialogDescription>
                        Describe what the workflow should do and AI will build it for you. This can take a minute.
                    </DialogDescription>
                </DialogHeader>

                <Textarea
                    autoFocus
                    disabled={isPending}
                    onChange={(event) => setPrompt(event.target.value)}
                    placeholder="When a new Gmail email arrives, post a summary to a Slack channel."
                    rows={5}
                    value={prompt}
                />

                <DialogFooter>
                    <Button disabled={isPending} label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={isPending || !prompt.trim()}
                        icon={isPending ? <Loader2Icon className="animate-spin" /> : <SparklesIcon />}
                        label={isPending ? 'Generating…' : 'Generate'}
                        onClick={handleGenerateClick}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default GenerateWorkflowDialog;
