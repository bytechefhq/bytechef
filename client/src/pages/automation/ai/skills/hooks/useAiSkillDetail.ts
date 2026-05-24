import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import downloadAiSkill from '@/pages/automation/ai/skills/utils/downloadAiSkill';
import {
    useAiSkillFileContentQuery,
    useAiSkillFilePathsQuery,
    useAiSkillQuery,
    useDeleteAiSkillMutation,
    useUpdateAiSkillContentMutation,
    useUpdateAiSkillMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface FileTreeNodeI {
    children: FileTreeNodeI[];
    name: string;
    path: string;
    type: 'directory' | 'file';
}

const FILE_LANGUAGE_MAP: Record<string, string> = {
    css: 'css',
    html: 'html',
    java: 'java',
    js: 'javascript',
    json: 'json',
    jsx: 'javascript',
    py: 'python',
    rb: 'ruby',
    sh: 'shell',
    ts: 'typescript',
    tsx: 'typescript',
    xml: 'xml',
    yaml: 'yaml',
    yml: 'yaml',
};

/** Transforms flat slash-separated file paths into a nested tree for the file explorer UI. */
function buildFileTree(paths: string[]): FileTreeNodeI[] {
    const root: FileTreeNodeI[] = [];

    for (const filePath of paths) {
        const parts = filePath.split('/');
        let currentLevel = root;

        for (let index = 0; index < parts.length; index++) {
            const part = parts[index];
            const isFile = index === parts.length - 1;
            const existingNode = currentLevel.find((node) => node.name === part);

            if (existingNode) {
                currentLevel = existingNode.children;
            } else {
                const newNode: FileTreeNodeI = {
                    children: [],
                    name: part,
                    path: isFile ? filePath : parts.slice(0, index + 1).join('/'),
                    type: isFile ? 'file' : 'directory',
                };

                currentLevel.push(newNode);
                currentLevel = newNode.children;
            }
        }
    }

    return root;
}

function getFileLanguage(filename: string): string {
    const extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

    return FILE_LANGUAGE_MAP[extension] || 'plaintext';
}

export type {FileTreeNodeI};

export default function useAiSkillDetail() {
    const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);
    const [isSaving, setIsSaving] = useState(false);

    const {closeSkillDetail, selectedSkillId} = useAiSkillsStore();

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {mutateAsync: updateAiSkillContent} = useUpdateAiSkillContentMutation();
    const {mutateAsync: updateAiSkill} = useUpdateAiSkillMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiSkill', {id: selectedSkillId}]}),
    });
    const {mutateAsync: deleteAiSkill} = useDeleteAiSkillMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiSkills']}),
    });

    const {data: skillData, isError: isSkillError} = useAiSkillQuery(
        {id: selectedSkillId ?? ''},
        {enabled: !!selectedSkillId}
    );

    const {data: filePathsData, isError: isFilePathsError} = useAiSkillFilePathsQuery(
        {id: selectedSkillId ?? ''},
        {enabled: !!selectedSkillId}
    );

    const {
        data: fileContentData,
        isError: isFileContentError,
        isLoading: isFileContentLoading,
    } = useAiSkillFileContentQuery(
        {id: selectedSkillId ?? '', path: selectedFilePath ?? ''},
        {enabled: !!selectedSkillId && !!selectedFilePath}
    );

    const filePaths = useMemo(() => filePathsData?.aiSkillFilePaths ?? [], [filePathsData]);
    const fileContent = useMemo(() => fileContentData?.aiSkillFileContent ?? '', [fileContentData]);
    const skill = skillData?.aiSkill;

    const fileTree = useMemo(() => buildFileTree(filePaths), [filePaths]);

    const editorLanguage = useMemo(
        () => (selectedFilePath ? getFileLanguage(selectedFilePath) : 'plaintext'),
        [selectedFilePath]
    );

    const isMarkdown = selectedFilePath ? selectedFilePath.toLowerCase().endsWith('.md') : false;

    useEffect(() => {
        if (skill?.name) {
            useAiSkillsStore.setState((state) => ({
                skillsHeaderInfo: {...state.skillsHeaderInfo, title: skill.name},
            }));
        }
    }, [skill?.name]);

    useEffect(() => {
        setSelectedFilePath(null);
    }, [selectedSkillId]);

    useEffect(() => {
        if (selectedFilePath !== null || filePaths.length === 0) {
            return;
        }

        const skillMdPath = filePaths.find((path) => path.split('/').pop()?.toLowerCase() === 'skill.md');

        if (skillMdPath) {
            setSelectedFilePath(skillMdPath);
        }
    }, [filePaths, selectedFilePath]);

    const handleBack = useCallback(() => {
        setSelectedFilePath(null);
        closeSkillDetail();
    }, [closeSkillDetail]);

    const handleDownload = useCallback(async () => {
        if (!selectedSkillId || !skill) {
            return;
        }

        try {
            await downloadAiSkill(selectedSkillId, skill.name);
        } catch (error) {
            toast.error('Failed to download skill', {
                description: error instanceof Error ? error.message : 'An unexpected error occurred',
            });
        }
    }, [selectedSkillId, skill]);

    const handleFileSelect = useCallback((path: string) => {
        setSelectedFilePath(path);
    }, []);

    const handleDelete = useCallback(async () => {
        if (!selectedSkillId) {
            return;
        }

        try {
            await deleteAiSkill({id: selectedSkillId});

            toast.success('Skill deleted');

            closeSkillDetail();
            navigate('/automation/ai/skills');
        } catch (error) {
            toast.error('Failed to delete skill', {
                description: error instanceof Error ? error.message : 'An unexpected error occurred',
            });
        }
    }, [closeSkillDetail, deleteAiSkill, navigate, selectedSkillId]);

    const handleUpdate = useCallback(
        async (name: string, description: string | null) => {
            if (!selectedSkillId || !skill) {
                return;
            }

            try {
                await updateAiSkill({description, id: selectedSkillId, name});

                toast.success('Skill updated');
            } catch (error) {
                toast.error('Failed to update skill', {
                    description: error instanceof Error ? error.message : 'An unexpected error occurred',
                });
            }
        },
        [selectedSkillId, skill, updateAiSkill]
    );

    const handleSaveContent = useCallback(
        async (content: string) => {
            if (!selectedSkillId) {
                return;
            }

            setIsSaving(true);

            try {
                await updateAiSkillContent({content, id: selectedSkillId, path: selectedFilePath});

                toast.success('Skill content saved');
            } catch (error) {
                toast.error('Failed to save skill content', {
                    description: error instanceof Error ? error.message : 'An unexpected error occurred',
                });
            } finally {
                setIsSaving(false);
            }
        },
        [selectedFilePath, selectedSkillId, updateAiSkillContent]
    );

    useEffect(() => {
        if (isSkillError) {
            toast.error('Failed to load skill details');
        }
    }, [isSkillError]);

    useEffect(() => {
        if (isFilePathsError) {
            toast.error('Failed to load skill file list');
        }
    }, [isFilePathsError]);

    useEffect(() => {
        if (isFileContentError) {
            toast.error('Failed to load file content');
        }
    }, [isFileContentError]);

    return {
        editorLanguage,
        fileContent,
        fileTree,
        handleBack,
        handleDelete,
        handleDownload,
        handleFileSelect,
        handleSaveContent,
        handleUpdate,
        isFileContentLoading,
        isMarkdown,
        isSaving,
        selectedFilePath,
        skill,
    };
}
