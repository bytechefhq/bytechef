import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {AiAutoMemoryPrincipalType, AiAutoMemoryType} from '@/shared/middleware/graphql';
import {TriangleAlertIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {toast} from 'sonner';

import {
    AI_AUTO_MEMORY_TYPES,
    AI_AUTO_MEMORY_TYPE_META,
    AiAutoMemoryDetailI,
    AiAutoMemoryI,
    AiAutoMemoryPatchI,
    AiAutoMemoryTypeType,
    useAiAutoMemoryDetailQuery,
    useUpdateAiAutoMemoryMutation,
} from '../hooks/useAiAutoMemories';

function toSingleLine(value: string): string {
    return value.replace(/[\r\n]+/g, ' ');
}

interface MemoryEditDialogProps {
    environmentId: number;
    memory: AiAutoMemoryI | null;
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

const MemoryEditDialog = ({environmentId, memory, onClose, open, workspaceId}: MemoryEditDialogProps) => {
    const [baseMemory, setBaseMemory] = useState<AiAutoMemoryDetailI | null>(null);
    const [content, setContent] = useState('');
    const [description, setDescription] = useState('');
    const [memoryType, setMemoryType] = useState<AiAutoMemoryTypeType>('USER');
    const [title, setTitle] = useState('');

    const {data: memoryDetail, refetch: refetchMemoryDetail} = useAiAutoMemoryDetailQuery(memory, workspaceId, open);

    const updateMemoryMutation = useUpdateAiAutoMemoryMutation();

    const changedSinceLoaded =
        memory !== null && baseMemory !== null && memory.id === baseMemory.id && memory.version !== baseMemory.version;

    const deletedSinceLoaded = memory === null && baseMemory !== null;

    const loadForm = useCallback((source: AiAutoMemoryDetailI) => {
        setBaseMemory(source);
        setContent(source.content);
        setDescription(toSingleLine(source.description ?? ''));
        setMemoryType(source.memoryType);
        setTitle(toSingleLine(source.title));
    }, []);

    const handleSave = () => {
        if (!baseMemory) {
            return;
        }

        const patch: AiAutoMemoryPatchI = {};

        if (title.trim() !== toSingleLine(baseMemory.title)) {
            patch.title = title.trim();
        }

        if (description !== toSingleLine(baseMemory.description ?? '')) {
            patch.description = description;
        }

        if (memoryType !== baseMemory.memoryType) {
            patch.memoryType = memoryType;
        }

        if (content !== baseMemory.content) {
            patch.content = content;
        }

        if (Object.keys(patch).length === 0) {
            onClose();

            return;
        }

        updateMemoryMutation.mutate(
            {
                input: {
                    content: patch.content,
                    description: patch.description,
                    environment: environmentId,
                    expectedVersion: baseMemory.version,
                    id: String(baseMemory.id),
                    memoryType: patch.memoryType as AiAutoMemoryType | undefined,
                    principal: {
                        principalId: baseMemory.principalId,
                        principalType: baseMemory.principalType as AiAutoMemoryPrincipalType,
                    },
                    title: patch.title,
                    workspaceId: String(workspaceId),
                },
            },
            {
                onSuccess: () => {
                    toast.success('Memory updated');

                    onClose();
                },
            }
        );
    };

    useEffect(() => {
        if (!open) {
            setBaseMemory(null);

            return;
        }

        if (memoryDetail && memoryDetail.id !== baseMemory?.id) {
            loadForm(memoryDetail);
        }
    }, [baseMemory, loadForm, memoryDetail, open]);

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent size="lg">
                <DialogMain>
                    <DialogHeader
                        description="Update the agent's long-term memory. The name cannot be changed here; ask an agent that uses the Auto Memory tool to rename the entry."
                        title="Edit memory"
                    />

                    {baseMemory && (
                        <DialogBody>
                            <div className="flex flex-col gap-4">
                                {deletedSinceLoaded && (
                                    <div
                                        className="flex items-center gap-3 rounded-md border px-4 py-2 text-sm text-muted-foreground"
                                        role="alert"
                                    >
                                        <TriangleAlertIcon className="size-4 shrink-0" />

                                        <span className="flex-1">
                                            This memory was deleted since you opened it, so your changes cannot be
                                            saved.
                                        </span>
                                    </div>
                                )}

                                {memory && changedSinceLoaded && (
                                    <div
                                        className="flex items-center gap-3 rounded-md border px-4 py-2 text-sm text-muted-foreground"
                                        role="alert"
                                    >
                                        <TriangleAlertIcon className="size-4 shrink-0" />

                                        <span className="flex-1">
                                            This memory was changed since you opened it. Reload it to edit the latest
                                            version; your unsaved changes will be discarded.
                                        </span>

                                        <Button
                                            label="Reload"
                                            onClick={() =>
                                                refetchMemoryDetail().then(
                                                    (result) => result.data && loadForm(result.data)
                                                )
                                            }
                                            size="sm"
                                            variant="outline"
                                        />
                                    </div>
                                )}

                                <fieldset className="border-0">
                                    <Label
                                        className="mb-1.5 block text-xs font-medium text-muted-foreground"
                                        htmlFor="name"
                                    >
                                        Name
                                    </Label>

                                    <Input disabled id="name" value={baseMemory.name} />
                                </fieldset>

                                <fieldset className="border-0">
                                    <Label
                                        className="mb-1.5 block text-xs font-medium text-muted-foreground"
                                        htmlFor="title"
                                    >
                                        Title
                                    </Label>

                                    <Input
                                        id="title"
                                        onChange={(event) => setTitle(toSingleLine(event.target.value))}
                                        value={title}
                                    />
                                </fieldset>

                                <fieldset className="border-0">
                                    <Label
                                        className="mb-1.5 block text-xs font-medium text-muted-foreground"
                                        htmlFor="description"
                                    >
                                        Description
                                    </Label>

                                    <Input
                                        id="description"
                                        onChange={(event) => setDescription(toSingleLine(event.target.value))}
                                        placeholder="Short one-line summary"
                                        value={description}
                                    />
                                </fieldset>

                                <fieldset className="border-0">
                                    <Label
                                        className="mb-1.5 block text-xs font-medium text-muted-foreground"
                                        htmlFor="memoryType"
                                    >
                                        Type
                                    </Label>

                                    <Select
                                        onValueChange={(value) => setMemoryType(value as AiAutoMemoryTypeType)}
                                        value={memoryType}
                                    >
                                        <SelectTrigger id="memoryType">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {AI_AUTO_MEMORY_TYPES.map((type) => (
                                                <SelectItem key={type} value={type}>
                                                    {AI_AUTO_MEMORY_TYPE_META[type].label}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </fieldset>

                                <fieldset className="border-0">
                                    <Label
                                        className="mb-1.5 block text-xs font-medium text-muted-foreground"
                                        htmlFor="content"
                                    >
                                        Content (Markdown)
                                    </Label>

                                    <Textarea
                                        className="min-h-64 font-mono text-xs"
                                        id="content"
                                        onChange={(event) => setContent(event.target.value)}
                                        rows={12}
                                        value={content}
                                    />
                                </fieldset>
                            </div>
                        </DialogBody>
                    )}

                    <DialogFooter>
                        <Button
                            disabled={updateMemoryMutation.isPending}
                            label="Cancel"
                            onClick={onClose}
                            variant="outline"
                        />

                        <Button
                            disabled={
                                !memory ||
                                changedSinceLoaded ||
                                updateMemoryMutation.isPending ||
                                title.trim().length === 0
                            }
                            label={updateMemoryMutation.isPending ? 'Saving...' : 'Save'}
                            onClick={handleSave}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default MemoryEditDialog;
