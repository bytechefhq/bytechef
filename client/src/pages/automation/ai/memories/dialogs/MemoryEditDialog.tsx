import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {AiAutoMemoryType} from '@/shared/middleware/graphql';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';

import {
    AiAutoMemoryI,
    AiAutoMemoryPatchI,
    AiAutoMemoryTypeType,
    useUpdateAiAutoMemoryMutation,
} from '../hooks/useAiAutoMemories';

interface MemoryEditDialogProps {
    memory: AiAutoMemoryI | null;
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

const MEMORY_TYPES: AiAutoMemoryTypeType[] = ['USER', 'FEEDBACK', 'PROJECT', 'REFERENCE'];

const MemoryEditDialog = ({memory, onClose, open, workspaceId}: MemoryEditDialogProps) => {
    const [content, setContent] = useState('');
    const [description, setDescription] = useState('');
    const [memoryType, setMemoryType] = useState<AiAutoMemoryTypeType>('USER');
    const [title, setTitle] = useState('');

    const updateMemoryMutation = useUpdateAiAutoMemoryMutation();

    useEffect(() => {
        if (memory) {
            setContent(memory.content);
            setDescription(memory.description ?? '');
            setMemoryType(memory.memoryType);
            setTitle(memory.title);
        }
    }, [memory]);

    const handleSave = async () => {
        if (!memory) {
            return;
        }

        const patch: AiAutoMemoryPatchI = {};

        if (title.trim() !== memory.title) {
            patch.title = title.trim();
        }

        if (description !== (memory.description ?? '')) {
            patch.description = description;
        }

        if (memoryType !== memory.memoryType) {
            patch.memoryType = memoryType;
        }

        if (content !== memory.content) {
            patch.content = content;
        }

        if (Object.keys(patch).length === 0) {
            onClose();

            return;
        }

        try {
            await updateMemoryMutation.mutateAsync({
                input: {
                    content: patch.content,
                    description: patch.description,
                    id: String(memory.id),
                    memoryType: patch.memoryType as AiAutoMemoryType | undefined,
                    title: patch.title,
                    workspaceId: String(workspaceId),
                },
            });

            toast.success('Memory updated');

            onClose();
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to update memory');
        }
    };

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent className="max-h-[85vh] overflow-hidden sm:max-w-3xl">
                <DialogHeader>
                    <DialogTitle>Edit memory</DialogTitle>

                    <DialogDescription>
                        Update the agent&apos;s long-term memory. The name is immutable; use the agent&apos;s rename
                        tool to change it.
                    </DialogDescription>
                </DialogHeader>

                {memory && (
                    <div className="flex flex-col gap-4 overflow-y-auto">
                        <fieldset className="border-0">
                            <Label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="name">
                                Name
                            </Label>

                            <Input disabled id="name" value={memory.name} />
                        </fieldset>

                        <fieldset className="border-0">
                            <Label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="title">
                                Title
                            </Label>

                            <Input id="title" onChange={(event) => setTitle(event.target.value)} value={title} />
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
                                onChange={(event) => setDescription(event.target.value)}
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
                                    {MEMORY_TYPES.map((type) => (
                                        <SelectItem key={type} value={type}>
                                            {type}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>

                        <fieldset className="border-0">
                            <Label className="mb-1.5 block text-xs font-medium text-muted-foreground" htmlFor="content">
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
                )}

                <DialogFooter>
                    <Button
                        disabled={updateMemoryMutation.isPending}
                        label="Cancel"
                        onClick={onClose}
                        variant="outline"
                    />

                    <Button
                        disabled={!memory || updateMemoryMutation.isPending || title.trim().length === 0}
                        label={updateMemoryMutation.isPending ? 'Saving...' : 'Save'}
                        onClick={handleSave}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default MemoryEditDialog;
