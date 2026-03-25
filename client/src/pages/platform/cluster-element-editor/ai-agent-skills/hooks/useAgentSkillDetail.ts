import {useAiAgentSkillsStore} from '@/pages/platform/cluster-element-editor/ai-agent-skills/stores/useAiAgentSkillsStore';
import downloadAgentSkill from '@/pages/platform/cluster-element-editor/ai-agent-skills/utils/downloadAgentSkill';
import {
    useAgentSkillFileContentQuery,
    useAgentSkillFilePathsQuery,
    useAgentSkillQuery,
} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo, useState} from 'react';
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

export default function useAgentSkillDetail() {
    const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null);

    const {closeSkillDetail, selectedSkillId} = useAiAgentSkillsStore();

    const {data: skillData, isError: isSkillError} = useAgentSkillQuery(
        {id: selectedSkillId!},
        {enabled: !!selectedSkillId}
    );

    const {data: filePathsData, isError: isFilePathsError} = useAgentSkillFilePathsQuery(
        {id: selectedSkillId!},
        {enabled: !!selectedSkillId}
    );

    const {
        data: fileContentData,
        isError: isFileContentError,
        isLoading: isFileContentLoading,
    } = useAgentSkillFileContentQuery(
        {id: selectedSkillId!, path: selectedFilePath!},
        {enabled: !!selectedSkillId && !!selectedFilePath}
    );

    const filePaths = useMemo(() => filePathsData?.agentSkillFilePaths ?? [], [filePathsData]);
    const fileContent = useMemo(() => fileContentData?.agentSkillFileContent ?? '', [fileContentData]);
    const skill = skillData?.agentSkill;

    const fileTree = useMemo(() => buildFileTree(filePaths), [filePaths]);

    const editorLanguage = useMemo(
        () => (selectedFilePath ? getFileLanguage(selectedFilePath) : 'plaintext'),
        [selectedFilePath]
    );

    const isMarkdown = selectedFilePath ? selectedFilePath.toLowerCase().endsWith('.md') : false;

    const handleBack = useCallback(() => {
        setSelectedFilePath(null);
        closeSkillDetail();
    }, [closeSkillDetail]);

    const handleDownload = useCallback(async () => {
        if (!selectedSkillId || !skill) {
            return;
        }

        try {
            await downloadAgentSkill(selectedSkillId, skill.name);
        } catch (error) {
            toast.error('Failed to download skill', {
                description: error instanceof Error ? error.message : 'An unexpected error occurred',
            });
        }
    }, [selectedSkillId, skill]);

    const handleFileSelect = useCallback((path: string) => {
        setSelectedFilePath(path);
    }, []);

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
        handleDownload,
        handleFileSelect,
        isFileContentLoading,
        isMarkdown,
        selectedFilePath,
        skill,
    };
}
