import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import {useCreateAgentSkillFromInstructionsMutation, useCreateAgentSkillMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import type React from 'react';
import {useCallback, useRef, useState} from 'react';
import {toast} from 'sonner';

const ACCEPTED_EXTENSIONS = ['.zip', '.skill', '.md'];

export default function useAgentSkillUploadForm() {
    const [dragActive, setDragActive] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<File[]>([]);

    const fileInputRef = useRef<HTMLInputElement>(null);
    const totalToUploadRef = useRef(0);
    const completedCountRef = useRef(0);
    const successCountRef = useRef(0);

    const {setSkillsView} = useAiAgentSkillsStore();

    const queryClient = useQueryClient();

    const createSkillMutation = useCreateAgentSkillMutation();
    const createSkillFromInstructionsMutation = useCreateAgentSkillFromInstructionsMutation();

    const handleUploadBatchComplete = useCallback(() => {
        queryClient.invalidateQueries({queryKey: ['agentSkills']});

        const totalCount = totalToUploadRef.current;
        const successCount = successCountRef.current;
        const failedCount = totalCount - successCount;

        if (failedCount === 0) {
            setSelectedFiles([]);
            setSkillsView('list');
        } else {
            toast.error(
                `${successCount} of ${totalCount} skills created, ${failedCount} failed. Fix issues and retry.`
            );
        }

        completedCountRef.current = 0;
        totalToUploadRef.current = 0;
        successCountRef.current = 0;
    }, [queryClient, setSkillsView]);

    const handleFilesSelect = useCallback((files: File[]) => {
        const validFiles = files.filter((file) => {
            const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

            if (!ACCEPTED_EXTENSIONS.includes(extension)) {
                toast.error(`Unsupported file type: ${file.name}`, {
                    description: `Accepted formats: ${ACCEPTED_EXTENSIONS.join(', ')}`,
                });

                return false;
            }

            return true;
        });

        if (validFiles.length > 0) {
            setSelectedFiles((previous) => [...previous, ...validFiles]);
        }
    }, []);

    const handleDrop = useCallback(
        (event: React.DragEvent<HTMLDivElement>) => {
            event.preventDefault();
            setDragActive(false);

            const droppedFiles = Array.from(event.dataTransfer.files);

            if (droppedFiles.length > 0) {
                handleFilesSelect(droppedFiles);
            }
        },
        [handleFilesSelect]
    );

    const handleDragOver = useCallback((event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        setDragActive(true);
    }, []);

    const handleDragLeave = useCallback(() => {
        setDragActive(false);
    }, []);

    const handleFileInputChange = useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            const files = event.target.files;

            if (files && files.length > 0) {
                handleFilesSelect(Array.from(files));
            }

            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        },
        [handleFilesSelect]
    );

    const handleRemoveFile = useCallback((index: number) => {
        setSelectedFiles((previous) => previous.filter((_, fileIndex) => fileIndex !== index));
    }, []);

    const handleUpload = useCallback(async () => {
        if (selectedFiles.length === 0) {
            toast.error('Please select at least one file to upload');

            return;
        }

        totalToUploadRef.current = selectedFiles.length;
        completedCountRef.current = 0;
        successCountRef.current = 0;

        for (const file of selectedFiles) {
            try {
                const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
                const fileNameWithoutExtension = file.name.replace(/\.[^/.]+$/, '');

                if (extension === '.md') {
                    const textContent = await file.text();

                    await createSkillFromInstructionsMutation.mutateAsync({
                        instructions: textContent,
                        name: fileNameWithoutExtension,
                    });
                } else {
                    const base64Content = await new Promise<string>((resolve, reject) => {
                        const reader = new FileReader();

                        reader.onload = () => {
                            const dataUrl = reader.result as string;
                            const base64 = dataUrl.split(',')[1];

                            resolve(base64);
                        };
                        reader.onerror = () => reject(reader.error);
                        reader.readAsDataURL(file);
                    });

                    await createSkillMutation.mutateAsync({
                        fileBytes: base64Content,
                        filename: file.name,
                        name: fileNameWithoutExtension,
                    });
                }

                successCountRef.current++;
            } catch (error) {
                toast.error(`Failed to create skill: ${file.name}`, {
                    description: error instanceof Error ? error.message : 'An unexpected error occurred',
                });
            } finally {
                completedCountRef.current++;

                if (completedCountRef.current >= totalToUploadRef.current) {
                    handleUploadBatchComplete();
                }
            }
        }
    }, [createSkillFromInstructionsMutation, createSkillMutation, handleUploadBatchComplete, selectedFiles]);

    return {
        acceptedExtensions: ACCEPTED_EXTENSIONS,
        dragActive,
        fileInputRef,
        handleDragLeave,
        handleDragOver,
        handleDrop,
        handleFileInputChange,
        handleRemoveFile,
        handleUpload,
        isUploadPending: createSkillMutation.isPending || createSkillFromInstructionsMutation.isPending,
        selectedFiles,
    };
}
