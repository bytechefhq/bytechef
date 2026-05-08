import Button from '@/components/Button/Button';
import {
    AiPromptVersionType as AiPromptVersionTypeEnum,
    useCreateAiPromptVersionMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-lg rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">New Version</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Content</label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                            onChange={(event) => setContent(event.target.value)}
                            placeholder="Enter your prompt template... Use {{variable}} for variables."
                            rows={6}
                            value={content}
                        />
                    </fieldset>

                    {detectedVariables.length > 0 && (
                        <fieldset className="border-0">
                            <label className="mb-1 block text-sm font-medium">Detected Variables</label>

                            <div className="flex flex-wrap gap-1">
                                {detectedVariables.map((variable) => (
                                    <span
                                        className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-800"
                                        key={variable}
                                    >
                                        {`{{${variable}}}`}
                                    </span>
                                ))}
                            </div>
                        </fieldset>
                    )}

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setType(event.target.value as AiPromptVersionTypeEnum)}
                            value={type}
                        >
                            <option value={AiPromptVersionTypeEnum.Text}>TEXT</option>

                            <option value={AiPromptVersionTypeEnum.Chat}>CHAT</option>
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Environment</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setEnvironment(event.target.value)}
                            value={environment}
                        >
                            {ENVIRONMENTS.map((env) => (
                                <option key={env} value={env}>
                                    {env.charAt(0).toUpperCase() + env.slice(1)}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Commit Message</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setCommitMessage(event.target.value)}
                            placeholder="Describe what changed in this version..."
                            value={commitMessage}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="flex items-center gap-2 text-sm font-medium">
                            <input
                                checked={active}
                                onChange={(event) => setActive(event.target.checked)}
                                type="checkbox"
                            />
                            Set as active for this environment
                        </label>
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!content || createVersionMutation.isPending}
                        label="Create Version"
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiPromptVersionDialog;
