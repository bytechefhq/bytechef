import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {useCreateAiGatewayProjectMutation, useUpdateAiGatewayProjectMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useRef, useState} from 'react';
import {toast} from 'sonner';

import {AiGatewayProjectType} from '../../types';
import AiGatewayProjectGuardrailsSection, {
    AiGatewayProjectGuardrailsSectionHandleI,
} from './AiGatewayProjectGuardrailsSection';

interface AiGatewayProjectDialogProps {
    onClose: () => void;
    project?: AiGatewayProjectType;
    workspaceId: string;
}

const AiGatewayProjectDialog = ({onClose, project, workspaceId}: AiGatewayProjectDialogProps) => {
    const [cacheTtlMinutes, setCacheTtlMinutes] = useState<number | undefined>(project?.cacheTtlMinutes ?? undefined);
    const [cachingEnabled, setCachingEnabled] = useState(project?.cachingEnabled ?? false);
    const [compressionEnabled, setCompressionEnabled] = useState(project?.compressionEnabled ?? false);
    const [description, setDescription] = useState(project?.description ?? '');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [logRetentionDays, setLogRetentionDays] = useState<number | undefined>(
        project?.logRetentionDays ?? undefined
    );
    const [name, setName] = useState(project?.name ?? '');
    const [retryMaxAttempts, setRetryMaxAttempts] = useState<number | undefined>(
        project?.retryMaxAttempts ?? undefined
    );
    const [slug, setSlug] = useState(project?.slug ?? '');
    const [timeoutSeconds, setTimeoutSeconds] = useState<number | undefined>(project?.timeoutSeconds ?? undefined);

    const guardrailsSectionRef = useRef<AiGatewayProjectGuardrailsSectionHandleI>(null);

    const queryClient = useQueryClient();

    const isEditMode = !!project;

    const createMutation = useCreateAiGatewayProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayProjects']});

            onClose();
        },
    });

    // No onSuccess here: closing the dialog and invalidating the list must wait until the
    // guardrails half (if any) has also been attempted — see handleSubmit.
    const updateMutation = useUpdateAiGatewayProjectMutation();

    const handleSubmit = useCallback(async () => {
        if (!isEditMode) {
            createMutation.mutate({
                input: {
                    cacheTtlMinutes: cacheTtlMinutes || undefined,
                    cachingEnabled,
                    compressionEnabled,
                    description: description || undefined,
                    logRetentionDays: logRetentionDays || undefined,
                    name,
                    retryMaxAttempts: retryMaxAttempts || undefined,
                    slug,
                    timeoutSeconds: timeoutSeconds || undefined,
                    workspaceId,
                },
            });

            return;
        }

        setIsSubmitting(true);

        try {
            await updateMutation.mutateAsync({
                id: project.id,
                input: {
                    cacheTtlMinutes: cacheTtlMinutes || undefined,
                    cachingEnabled,
                    compressionEnabled,
                    description: description || undefined,
                    logRetentionDays: logRetentionDays || undefined,
                    name,
                    retryMaxAttempts: retryMaxAttempts || undefined,
                    slug,
                    timeoutSeconds: timeoutSeconds || undefined,
                },
            });

            // The project row is now persisted. If the guardrails half fails from here on, that
            // is a genuine partial-success state that must be surfaced honestly and must not
            // close the dialog, since closing would discard the still-unsaved guardrail edits.
            const guardrailsSaved = (await guardrailsSectionRef.current?.save()) ?? true;

            if (!guardrailsSaved) {
                toast.error(
                    'Project details were saved, but the guardrail changes failed to save. Click Save again to retry.'
                );

                return;
            }

            queryClient.invalidateQueries({queryKey: ['aiGatewayProjects']});

            onClose();
        } catch {
            // The project update itself failed before anything was sent to the guardrails
            // endpoint, so no partial state exists yet — the fetch interceptor already surfaced
            // the error toast, and it is safe for the user to just retry Save.
        } finally {
            setIsSubmitting(false);
        }
    }, [
        cacheTtlMinutes,
        cachingEnabled,
        compressionEnabled,
        createMutation,
        description,
        isEditMode,
        logRetentionDays,
        name,
        onClose,
        project,
        queryClient,
        retryMaxAttempts,
        slug,
        timeoutSeconds,
        updateMutation,
        workspaceId,
    ]);

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined} className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{isEditMode ? 'Edit Project' : 'Add Project'}</DialogTitle>
                </DialogHeader>

                <div className="max-h-[60vh] space-y-4 overflow-y-auto">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectName">
                            Name
                        </Label>

                        <Input
                            id="aiGatewayProjectName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Project"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectSlug">
                            Slug
                        </Label>

                        <Input
                            id="aiGatewayProjectSlug"
                            onChange={(event) => setSlug(event.target.value)}
                            placeholder="my-project"
                            value={slug}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectDescription">
                            Description (optional)
                        </Label>

                        <Input
                            id="aiGatewayProjectDescription"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Project description"
                            value={description}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectRoutingPolicyId">
                            Routing Policy ID (optional)
                        </Label>

                        <Input
                            disabled={true}
                            id="aiGatewayProjectRoutingPolicyId"
                            placeholder="Select from Routing Policies"
                        />
                    </fieldset>

                    <fieldset className="flex items-center gap-2 border-0">
                        <Checkbox
                            checked={compressionEnabled}
                            id="aiGatewayProjectCompressionEnabled"
                            onCheckedChange={(checked) => setCompressionEnabled(checked === true)}
                        />

                        <Label htmlFor="aiGatewayProjectCompressionEnabled">Compression Enabled</Label>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectRetryMaxAttempts">
                            Retry Max Attempts (optional)
                        </Label>

                        <Input
                            id="aiGatewayProjectRetryMaxAttempts"
                            min={0}
                            onChange={(event) =>
                                setRetryMaxAttempts(event.target.value ? Number(event.target.value) : undefined)
                            }
                            placeholder="3"
                            type="number"
                            value={retryMaxAttempts ?? ''}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectTimeoutSeconds">
                            Timeout Seconds (optional)
                        </Label>

                        <Input
                            id="aiGatewayProjectTimeoutSeconds"
                            min={0}
                            onChange={(event) =>
                                setTimeoutSeconds(event.target.value ? Number(event.target.value) : undefined)
                            }
                            placeholder="30"
                            type="number"
                            value={timeoutSeconds ?? ''}
                        />
                    </fieldset>

                    <fieldset className="flex items-center gap-2 border-0">
                        <Checkbox
                            checked={cachingEnabled}
                            id="aiGatewayProjectCachingEnabled"
                            onCheckedChange={(checked) => setCachingEnabled(checked === true)}
                        />

                        <Label htmlFor="aiGatewayProjectCachingEnabled">Caching Enabled</Label>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectCacheTtlMinutes">
                            Cache TTL Minutes (optional)
                        </Label>

                        <Input
                            id="aiGatewayProjectCacheTtlMinutes"
                            min={0}
                            onChange={(event) =>
                                setCacheTtlMinutes(event.target.value ? Number(event.target.value) : undefined)
                            }
                            placeholder="60"
                            type="number"
                            value={cacheTtlMinutes ?? ''}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayProjectLogRetentionDays">
                            Log Retention Days (optional)
                        </Label>

                        <Input
                            id="aiGatewayProjectLogRetentionDays"
                            min={0}
                            onChange={(event) =>
                                setLogRetentionDays(event.target.value ? Number(event.target.value) : undefined)
                            }
                            placeholder="30"
                            type="number"
                            value={logRetentionDays ?? ''}
                        />
                    </fieldset>

                    {isEditMode && project?.id && (
                        <AiGatewayProjectGuardrailsSection projectId={project.id} ref={guardrailsSectionRef} />
                    )}
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || !slug || createMutation.isPending || isSubmitting}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiGatewayProjectDialog;
