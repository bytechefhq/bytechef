import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import AiSkillDeleteAlertDialog from '@/pages/automation/ai/skills/components/AiSkillDeleteAlertDialog';
import AiSkillFileAddDialog from '@/pages/automation/ai/skills/components/AiSkillFileAddDialog';
import AiSkillFileDeleteAlertDialog from '@/pages/automation/ai/skills/components/AiSkillFileDeleteAlertDialog';
import useAiSkillDetail, {type FileTreeNodeI} from '@/pages/automation/ai/skills/hooks/useAiSkillDetail';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import parseFrontmatter from '@/pages/automation/ai/skills/utils/parseFrontmatter';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {EditorContent, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {FileIcon, FileTextIcon, FolderIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {Markdown} from 'tiptap-markdown';

const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface FileTreeNodeProps {
    node: FileTreeNodeI;
    onRemove: (path: string) => void;
    onSelect: (path: string) => void;
    selectedPath: string | null;
}

const FileTreeNode = ({node, onRemove, onSelect, selectedPath}: FileTreeNodeProps) => {
    const isSelected = node.type === 'file' && node.path === selectedPath;

    if (node.type === 'directory') {
        return (
            <div>
                <div className="flex items-center gap-1.5 px-2 py-1 text-sm text-gray-600">
                    <FolderIcon className="size-4" />

                    <span>{node.name}</span>
                </div>

                <div className="ml-3">
                    {node.children.map((child) => (
                        <FileTreeNode
                            key={child.path}
                            node={child}
                            onRemove={onRemove}
                            onSelect={onSelect}
                            selectedPath={selectedPath}
                        />
                    ))}
                </div>
            </div>
        );
    }

    const isSkillMd = node.path.toLowerCase() === 'skill.md';

    return (
        <div className="group flex items-center">
            <Button
                className={twMerge(
                    'h-auto min-w-0 flex-1 justify-start gap-1.5 rounded px-2 py-1 text-left text-sm font-normal',
                    isSelected &&
                        'bg-blue-50 text-blue-700 hover:bg-blue-50 hover:text-blue-700 active:bg-blue-50 active:text-blue-700'
                )}
                onClick={() => onSelect(node.path)}
                size="xs"
                type="button"
                variant="ghost"
            >
                {node.name.toLowerCase().endsWith('.md') ? (
                    <FileTextIcon className="size-4 shrink-0" />
                ) : (
                    <FileIcon className="size-4 shrink-0" />
                )}

                <span className="truncate">{node.name}</span>
            </Button>

            {!isSkillMd && (
                <Button
                    aria-label={`Remove ${node.name}`}
                    className="h-auto shrink-0 rounded p-1 text-gray-400 opacity-0 group-hover:opacity-100 hover:text-red-600"
                    onClick={(event) => {
                        event.stopPropagation();

                        onRemove(node.path);
                    }}
                    size="xs"
                    type="button"
                    variant="ghost"
                >
                    <TrashIcon className="size-3.5" />
                </Button>
            )}
        </div>
    );
};

const FrontmatterTable = ({frontmatter}: {frontmatter: Record<string, string>}) => (
    <table className="mb-6 w-full border-collapse text-sm">
        <tbody>
            {Object.entries(frontmatter).map(([key, value]) => (
                <tr className="border-b border-b-border/50" key={key}>
                    <td className="py-2 pr-4 font-medium text-gray-600">{key}</td>

                    <td className="py-2 text-gray-900">{value}</td>
                </tr>
            ))}
        </tbody>
    </table>
);

interface MarkdownViewerProps {
    content: string;
    editable: boolean;
    onContentChange: (markdown: string) => void;
}

const MarkdownViewer = ({content, editable, onContentChange}: MarkdownViewerProps) => {
    const {body, frontmatter, rawFrontmatter} = parseFrontmatter(content);

    const rawFrontmatterRef = useRef<string | null>(rawFrontmatter);

    rawFrontmatterRef.current = rawFrontmatter;

    const editor = useEditor({
        content: body,
        editable,
        extensions: [StarterKit, Markdown],
        onUpdate: ({editor: updatedEditor}) => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const markdown = (updatedEditor.storage as any).markdown.getMarkdown() as string;

            const fullContent = rawFrontmatterRef.current
                ? `---\n${rawFrontmatterRef.current}\n---\n\n${markdown}`
                : markdown;

            onContentChange(fullContent);
        },
    });

    useEffect(() => {
        if (editor && body) {
            editor.commands.setContent(body);
        }
    }, [body, editor]);

    useEffect(() => {
        editor?.setEditable(editable);
    }, [editable, editor]);

    return (
        <div className="overflow-y-auto px-6 pt-0 pb-6">
            {frontmatter && <FrontmatterTable frontmatter={frontmatter} />}

            <div className="prose prose-sm max-w-none">
                <EditorContent editor={editor} />
            </div>
        </div>
    );
};

const AiSkillDetail = () => {
    const [showAddFileDialog, setShowAddFileDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [fileToRemove, setFileToRemove] = useState<string | null>(null);
    const [isContentDirty, setIsContentDirty] = useState(false);

    const latestContentRef = useRef('');

    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const setContext = useCopilotStore((state) => state.setContext);

    const setCanSave = useAiSkillDetailToolbarStore((state) => state.setCanSave);
    const setCanToggleView = useAiSkillDetailToolbarStore((state) => state.setCanToggleView);
    const setHandlers = useAiSkillDetailToolbarStore((state) => state.setHandlers);
    const setIsSavingStore = useAiSkillDetailToolbarStore((state) => state.setIsSaving);
    const setViewMode = useAiSkillDetailToolbarStore((state) => state.setViewMode);
    const viewMode = useAiSkillDetailToolbarStore((state) => state.viewMode);
    const resetToolbar = useAiSkillDetailToolbarStore((state) => state.resetToolbar);

    const {
        editorLanguage,
        fileContent,
        filePaths,
        fileTree,
        handleAddFile,
        handleDelete,
        handleDownload,
        handleFileSelect,
        handleRemoveFile,
        handleSaveContent,
        isFileContentLoading,
        isMarkdown,
        isSaving,
        selectedFilePath,
        skill,
    } = useAiSkillDetail();

    useEffect(() => {
        setIsContentDirty(false);
        latestContentRef.current = '';

        setViewMode('preview');
    }, [selectedFilePath, setViewMode]);

    useEffect(() => {
        setCanToggleView(selectedFilePath != null && isMarkdown);
    }, [isMarkdown, selectedFilePath, setCanToggleView]);

    useEffect(() => {
        const openCopilot = () => {
            setContext({mode: MODE.BUILD, parameters: {}, source: Source.SKILLS});

            setCopilotPanelOpen(true);
        };

        const save = async () => {
            await handleSaveContent(latestContentRef.current);

            setIsContentDirty(false);
        };

        setHandlers({
            onCopilot: openCopilot,
            onDelete: () => setShowDeleteDialog(true),
            onDownload: handleDownload,
            onSave: save,
        });
    }, [handleDownload, handleSaveContent, setCopilotPanelOpen, setContext, setHandlers]);

    useEffect(() => {
        setCanSave(selectedFilePath != null && isContentDirty && !isSaving);
    }, [isContentDirty, isSaving, selectedFilePath, setCanSave]);

    useEffect(() => {
        setIsSavingStore(isSaving);
    }, [isSaving, setIsSavingStore]);

    useEffect(() => {
        return () => resetToolbar();
    }, [resetToolbar]);

    if (!skill) {
        return (
            <div className="flex flex-1 items-center justify-center">
                <LoadingIcon />
            </div>
        );
    }

    return (
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
            <div className="flex min-h-0 flex-1 overflow-hidden">
                <div className="w-60 shrink-0 border-r border-r-border/50 py-2 pr-2">
                    <div className="mb-1 flex items-center justify-between px-2">
                        <span className="text-xs font-medium text-gray-400 uppercase">Files</span>

                        <Button
                            aria-label="Add file"
                            className="h-auto rounded p-1 text-gray-400 hover:text-gray-700"
                            onClick={() => setShowAddFileDialog(true)}
                            size="xs"
                            type="button"
                            variant="ghost"
                        >
                            <PlusIcon className="size-3.5" />
                        </Button>
                    </div>

                    {fileTree.map((node) => (
                        <FileTreeNode
                            key={node.path}
                            node={node}
                            onRemove={setFileToRemove}
                            onSelect={handleFileSelect}
                            selectedPath={selectedFilePath}
                        />
                    ))}
                </div>

                <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
                    {selectedFilePath ? (
                        <div className="relative min-h-0 flex-1">
                            {isFileContentLoading ? (
                                <div className="flex items-center justify-center p-8">
                                    <LoadingIcon />
                                </div>
                            ) : isMarkdown && viewMode === 'preview' ? (
                                <div className="absolute inset-0 overflow-y-auto">
                                    <MarkdownViewer
                                        content={isContentDirty ? latestContentRef.current : fileContent}
                                        editable={false}
                                        onContentChange={() => {}}
                                    />
                                </div>
                            ) : (
                                <div className="absolute inset-0">
                                    <Suspense
                                        fallback={
                                            <div className="flex items-center justify-center p-8">
                                                <LoadingIcon />
                                            </div>
                                        }
                                    >
                                        <MonacoEditorWrapper
                                            defaultLanguage={isMarkdown ? 'markdown' : editorLanguage}
                                            onChange={(value) => {
                                                setIsContentDirty(true);
                                                latestContentRef.current = value ?? '';
                                            }}
                                            onMount={() => {}}
                                            options={{
                                                automaticLayout: true,
                                                folding: true,
                                                lineNumbers: 'on',
                                                minimap: {enabled: false},
                                                scrollBeyondLastLine: false,
                                                wordWrap: 'on',
                                            }}
                                            value={isContentDirty ? latestContentRef.current : fileContent}
                                        />
                                    </Suspense>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="flex flex-1 items-center justify-center text-sm text-gray-400">
                            Select a file to view its contents
                        </div>
                    )}
                </div>
            </div>

            {showDeleteDialog && (
                <AiSkillDeleteAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={async () => {
                        setShowDeleteDialog(false);

                        await handleDelete();
                    }}
                />
            )}

            {fileToRemove && (
                <AiSkillFileDeleteAlertDialog
                    fileName={fileToRemove}
                    onClose={() => setFileToRemove(null)}
                    onDelete={async () => {
                        const path = fileToRemove;

                        setFileToRemove(null);

                        await handleRemoveFile(path);
                    }}
                />
            )}

            {showAddFileDialog && (
                <AiSkillFileAddDialog
                    existingPaths={filePaths}
                    onAdd={async (path) => {
                        setShowAddFileDialog(false);

                        await handleAddFile(path);
                    }}
                    onClose={() => setShowAddFileDialog(false)}
                />
            )}
        </div>
    );
};

export default AiSkillDetail;
