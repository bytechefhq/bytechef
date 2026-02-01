import {useToast} from '@/hooks/use-toast';
import {KnowledgeBase, useUpdateKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

interface UseEditKnowledgeBaseDialogProps {
    knowledgeBase: KnowledgeBase;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
}

export default function useEditKnowledgeBaseDialog({
    knowledgeBase,
    onOpenChange,
    open: controlledOpen,
}: UseEditKnowledgeBaseDialogProps) {
    const [internalOpen, setInternalOpen] = useState(false);
    const [name, setName] = useState(knowledgeBase.name);
    const [description, setDescription] = useState(knowledgeBase.description || '');

    const isControlled = controlledOpen !== undefined;
    const open = isControlled ? controlledOpen : internalOpen;

    const queryClient = useQueryClient();
    const {toast} = useToast();

    const updateMutation = useUpdateKnowledgeBaseMutation({
        onError: () => {
            toast({description: 'Failed to save settings. Please try again.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBase.id}]});
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast({description: 'Settings saved successfully.'});

            handleOpenChange(false);
        },
    });

    useEffect(() => {
        if (open) {
            setName(knowledgeBase.name);
            setDescription(knowledgeBase.description || '');
        }
    }, [open, knowledgeBase.name, knowledgeBase.description]);

    const handleNameChange = (newName: string) => {
        setName(newName);
    };

    const handleDescriptionChange = (newDescription: string) => {
        setDescription(newDescription);
    };

    const handleOpenChange = (newOpen: boolean) => {
        if (isControlled) {
            onOpenChange?.(newOpen);
        } else {
            setInternalOpen(newOpen);
        }
    };

    const handleCancel = () => {
        handleOpenChange(false);
    };

    const handleSave = () => {
        updateMutation.mutate({
            id: knowledgeBase.id,
            knowledgeBase: {
                description: description.trim() || undefined,
                maxChunkSize: knowledgeBase.maxChunkSize,
                minChunkSizeChars: knowledgeBase.minChunkSizeChars,
                name: name.trim(),
                overlap: knowledgeBase.overlap,
            },
        });
    };

    const canSubmit = name.trim().length > 0;

    return {
        canSubmit,
        description,
        handleCancel,
        handleDescriptionChange,
        handleNameChange,
        handleOpenChange,
        handleSave,
        isPending: updateMutation.isPending,
        name,
        open,
    };
}
