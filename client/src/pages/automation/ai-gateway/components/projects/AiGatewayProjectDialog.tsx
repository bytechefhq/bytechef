import Button from '@/components/Button/Button';
import {useCreateAiGatewayProjectMutation, useUpdateAiGatewayProjectMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiGatewayProjectType} from '../../types';

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
    const [logRetentionDays, setLogRetentionDays] = useState<number | undefined>(
        project?.logRetentionDays ?? undefined
    );
    const [name, setName] = useState(project?.name ?? '');
    const [retryMaxAttempts, setRetryMaxAttempts] = useState<number | undefined>(
        project?.retryMaxAttempts ?? undefined
    );
    const [slug, setSlug] = useState(project?.slug ?? '');
    const [timeoutSeconds, setTimeoutSeconds] = useState<number | undefined>(project?.timeoutSeconds ?? undefined);

    const queryClient = useQueryClient();

    const isEditMode = !!project;

    const createMutation = useCreateAiGatewayProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayProjects']});

            onClose();
        },
    });

    const updateMutation = useUpdateAiGatewayProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayProjects']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
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
        } else {
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
        project,
        retryMaxAttempts,
        slug,
        timeoutSeconds,
        updateMutation,
        workspaceId,
    ]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Project' : 'Add Project'}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="max-h-[60vh] space-y-4 overflow-y-auto">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Project"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Slug</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setSlug(event.target.value)}
                            placeholder="my-project"
                            value={slug}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Description (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Project description"
                            value={description}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Routing Policy ID (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            disabled={true}
                            placeholder="Select from Routing Policies"
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="flex items-center gap-2 text-sm font-medium">
                            <input
                                checked={compressionEnabled}
                                onChange={(event) => setCompressionEnabled(event.target.checked)}
                                type="checkbox"
                            />
                            Compression Enabled
                        </label>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Retry Max Attempts (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
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
                        <label className="mb-1 block text-sm font-medium">Timeout Seconds (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            min={0}
                            onChange={(event) =>
                                setTimeoutSeconds(event.target.value ? Number(event.target.value) : undefined)
                            }
                            placeholder="30"
                            type="number"
                            value={timeoutSeconds ?? ''}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="flex items-center gap-2 text-sm font-medium">
                            <input
                                checked={cachingEnabled}
                                onChange={(event) => setCachingEnabled(event.target.checked)}
                                type="checkbox"
                            />
                            Caching Enabled
                        </label>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Cache TTL Minutes (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
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
                        <label className="mb-1 block text-sm font-medium">Log Retention Days (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            min={0}
                            onChange={(event) =>
                                setLogRetentionDays(event.target.value ? Number(event.target.value) : undefined)
                            }
                            placeholder="30"
                            type="number"
                            value={logRetentionDays ?? ''}
                        />
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || !slug || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiGatewayProjectDialog;
