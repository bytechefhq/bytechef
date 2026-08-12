import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useUpdateWorkspaceSystemPromptMutation, useWorkspaceSystemPromptQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';

const MAX_LENGTH = 4000;

const WorkspaceSystemPrompt = () => {
    const [prompt, setPrompt] = useState('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useWorkspaceSystemPromptQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : ''},
        {enabled: currentWorkspaceId != null}
    );

    const updateMutation = useUpdateWorkspaceSystemPromptMutation({
        onSuccess: () => {
            toast.success('Workspace system prompt saved');

            queryClient.invalidateQueries({queryKey: ['workspaceSystemPrompt']});
        },
    });

    useEffect(() => {
        setPrompt(data?.workspaceSystemPrompt?.prompt ?? '');
    }, [data]);

    const handleSave = () => {
        if (currentWorkspaceId == null) {
            return;
        }

        const trimmed = prompt.trim();

        updateMutation.mutate({
            input: {
                prompt: trimmed.length > 0 ? trimmed : undefined,
                workspaceId: String(currentWorkspaceId),
            },
        });
    };

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
                <div className="space-y-6 py-6">
                    <p className="text-sm text-muted-foreground">
                        Standing instructions appended to every AI agent operating in this workspace: AI Hub chat
                        (copilot and personal agents), its specialist subagents, and canvas AI Agent runs. Each
                        agent&apos;s own prompt and safety rules always come first — these instructions cannot override
                        them.
                    </p>

                    <fieldset className="space-y-2 border-0 p-0">
                        <Label htmlFor="workspace-system-prompt">Workspace system prompt</Label>

                        <Textarea
                            className="min-h-64 font-mono text-sm"
                            id="workspace-system-prompt"
                            maxLength={MAX_LENGTH}
                            onChange={(event) => setPrompt(event.target.value)}
                            placeholder="Example: Always answer in German. Our fiscal year starts in February."
                            value={prompt}
                        />

                        <p className="text-right text-xs text-muted-foreground">
                            {prompt.length} / {MAX_LENGTH}
                        </p>
                    </fieldset>

                    <div className="flex justify-end">
                        <Button
                            disabled={updateMutation.isPending}
                            label={updateMutation.isPending ? 'Saving...' : 'Save'}
                            onClick={handleSave}
                        />
                    </div>
                </div>
            </div>
        </PageLoader>
    );
};

export default WorkspaceSystemPrompt;
