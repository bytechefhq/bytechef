import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Checkbox} from '@/components/ui/checkbox';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {
    AiPromptVersionType as AiPromptVersionTypeEnum,
    useCreateAiPromptVersionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';

interface AiPromptVersionDialogProps {
    onClose: () => void;
    promptId: string;
}

const ENVIRONMENTS = ['production', 'staging', 'development'];

const AiPromptVersionDialog = ({onClose, promptId}: AiPromptVersionDialogProps) => {
    const [active, setActive] = useState(false);
    const [commitMessage, setCommitMessage] = useState('');
    const [content, setContent] = useState('');
    const [environment, setEnvironment] = useState('development');
    const [type, setType] = useState<AiPromptVersionTypeEnum>(AiPromptVersionTypeEnum.Text);

    const queryClient = useQueryClient();

    const detectedVariables = useMemo(() => {
        const matches = content.match(/\{\{(\w+)\}\}/g);

        if (!matches) {
            return [];
        }

        return [...new Set(matches.map((match) => match.replace(/\{\{|\}\}/g, '')))];
    }, [content]);

    const createVersionMutation = useCreateAiPromptVersionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompt']});
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        createVersionMutation.mutate({
            input: {
                active,
                commitMessage: commitMessage || undefined,
                content,
                environment,
                promptId,
                type,
                variables: detectedVariables.length > 0 ? JSON.stringify(detectedVariables) : undefined,
            },
        });
    }, [active, commitMessage, content, createVersionMutation, detectedVariables, environment, promptId, type]);

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined} className="max-w-lg">
                <DialogHeader>
                    <DialogTitle>New Version</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-version-content">
                            Content
                        </Label>

                        <Textarea
                            className="font-mono"
                            id="ai-prompt-version-content"
                            onChange={(event) => setContent(event.target.value)}
                            placeholder="Enter your prompt template... Use {{variable}} for variables."
                            rows={6}
                            value={content}
                        />
                    </fieldset>

                    {detectedVariables.length > 0 && (
                        <fieldset className="border-0">
                            <legend className="mb-1 block text-sm font-medium">Detected Variables</legend>

                            <div className="flex flex-wrap gap-1">
                                {detectedVariables.map((variable) => (
                                    <span
                                        className="rounded-full bg-surface-brand-secondary px-2 py-0.5 text-xs font-medium text-content-brand-primary"
                                        key={variable}
                                    >
                                        {`{{${variable}}}`}
                                    </span>
                                ))}
                            </div>
                        </fieldset>
                    )}

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-version-type">
                            Type
                        </Label>

                        <Select onValueChange={(value) => setType(value as AiPromptVersionTypeEnum)} value={type}>
                            <SelectTrigger id="ai-prompt-version-type">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value={AiPromptVersionTypeEnum.Text}>TEXT</SelectItem>

                                <SelectItem value={AiPromptVersionTypeEnum.Chat}>CHAT</SelectItem>
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-version-environment">
                            Environment
                        </Label>

                        <Select onValueChange={setEnvironment} value={environment}>
                            <SelectTrigger id="ai-prompt-version-environment">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {ENVIRONMENTS.map((env) => (
                                    <SelectItem key={env} value={env}>
                                        {env.charAt(0).toUpperCase() + env.slice(1)}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-version-commit-message">
                            Commit Message
                        </Label>

                        <Input
                            id="ai-prompt-version-commit-message"
                            onChange={(event) => setCommitMessage(event.target.value)}
                            placeholder="Describe what changed in this version..."
                            value={commitMessage}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <div className="flex items-center gap-2">
                            <Checkbox
                                checked={active}
                                id="ai-prompt-version-active"
                                onCheckedChange={(checked) => setActive(checked === true)}
                            />

                            <Label htmlFor="ai-prompt-version-active">Set as active for this environment</Label>
                        </div>
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!content || createVersionMutation.isPending}
                        label="Create Version"
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiPromptVersionDialog;
