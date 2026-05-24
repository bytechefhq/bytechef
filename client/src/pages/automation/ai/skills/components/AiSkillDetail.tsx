import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import AiSkillEditDialog from '@/pages/automation/ai/skills/components/AiSkillEditDialog';
import useAiSkillDetail, {type FileTreeNodeI} from '@/pages/automation/ai/skills/hooks/useAiSkillDetail';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {EditorContent, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {FileIcon, FileTextIcon, FolderIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {Markdown} from 'tiptap-markdown';

const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface FileTreeNodeProps {
    node: FileTreeNodeI;
    onSelect: (path: string) => void;
    selectedPath: string | null;
}

const FileTreeNode = ({node, onSelect, selectedPath}: FileTreeNodeProps) => {
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
                        <FileTreeNode key={child.path} node={child} onSelect={onSelect} selectedPath={selectedPath} />
                    ))}
                </div>
            </div>
        );
    }

    return (
        <Button
            className={twMerge(
                'h-auto w-full justify-start gap-1.5 rounded px-2 py-1 text-left text-sm font-normal',
                isSelected &&
                    'bg-blue-50 text-blue-700 hover:bg-blue-50 hover:text-blue-700 active:bg-blue-50 active:text-blue-700'
            )}
            onClick={() => onSelect(node.path)}
            size="xs"
            type="button"
            variant="ghost"
        >
            {node.name.toLowerCase().endsWith('.md') ? (
                <FileTextIcon className="size-4" />
            ) : (
                <FileIcon className="size-4" />
            )}

            <span>{node.name}</span>
        </Button>
    );
};

interface ParsedFrontmatterI {
    body: string;
    frontmatter: Record<string, string> | null;
    rawFrontmatter: string | null;
}

function parseFrontmatter(content: string): ParsedFrontmatterI {
    const frontmatterMatch = content.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);

    if (!frontmatterMatch) {
        return {body: content, frontmatter: null, rawFrontmatter: null};
    }

    const frontmatterLines = frontmatterMatch[1].split('\n');
    const frontmatter: Record<string, string> = {};

    for (const line of frontmatterLines) {
        const colonIndex = line.indexOf(':');

        if (colonIndex > 0) {
            const key = line.substring(0, colonIndex).trim();
            const value = line.substring(colonIndex + 1).trim();

            frontmatter[key] = value.replace(/^"|"$/g, '');
        }
    }

    return {body: frontmatterMatch[2].trim(), frontmatter, rawFrontmatter: frontmatterMatch[1]};
}

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
    onContentChange: (markdown: string) => void;
}

const MarkdownViewer = ({content, onContentChange}: MarkdownViewerProps) => {
    const {body, frontmatter, rawFrontmatter} = parseFrontmatter(content);

    // The editor only sees the body, so onUpdate emits body-only markdown. Keep the raw frontmatter
    // in a ref so the saved file preserves it verbatim — re-stringifying from the parsed object would
    // drop quoting, comments, and any non key:value syntax (arrays, nested keys) the parser doesn't
    // round-trip.
    const rawFrontmatterRef = useRef<string | null>(rawFrontmatter);

    rawFrontmatterRef.current = rawFrontmatter;

    const editor = useEditor({
        content: body,
        editable: true,
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

    return (
        <div className="overflow-y-auto px-6 pb-6 pt-0">
            {frontmatter && <FrontmatterTable frontmatter={frontmatter} />}

            <div className="prose prose-sm max-w-none">
                <EditorContent editor={editor} />
            </div>
        </div>
    );
};

const AiSkillDetail = () => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [isContentDirty, setIsContentDirty] = useState(false);

    const latestContentRef = useRef('');

    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const setContext = useCopilotStore((state) => state.setContext);

    const setCanSave = useAiSkillDetailToolbarStore((state) => state.setCanSave);
    const setHandlers = useAiSkillDetailToolbarStore((state) => state.setHandlers);
    const setIsSavingStore = useAiSkillDetailToolbarStore((state) => state.setIsSaving);
    const resetToolbar = useAiSkillDetailToolbarStore((state) => state.resetToolbar);

    const {
        editorLanguage,
        fileContent,
        fileTree,
        handleDownload,
        handleFileSelect,
        handleSaveContent,
        handleUpdate,
        isFileContentLoading,
        isMarkdown,
        isSaving,
        selectedFilePath,
        skill,
    } = useAiSkillDetail();

    useEffect(() => {
        setIsContentDirty(false);
        latestContentRef.current = '';
    }, [selectedFilePath]);

    // Publish the toolbar handlers + flags so the page header (rendered in AiSkills) can show the
    // action buttons aligned with the title. Re-runs whenever any of the captured values change so
    // the header's button closures stay current.
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
            onDownload: handleDownload,
            onEdit: () => setShowEditDialog(true),
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
                    {fileTree.map((node) => (
                        <FileTreeNode
                            key={node.path}
                            node={node}
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
                            ) : isMarkdown ? (
                                <div className="absolute inset-0 overflow-y-auto">
                                    <MarkdownViewer
                                        content={fileContent}
                                        onContentChange={(markdown) => {
                                            setIsContentDirty(true);
                                            latestContentRef.current = markdown;
                                        }}
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
                                            defaultLanguage={editorLanguage}
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
                                            value={fileContent}
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

            {showEditDialog && (
                <AiSkillEditDialog
                    currentDescription={skill.description}
                    currentName={skill.name}
                    onClose={() => setShowEditDialog(false)}
                    onSave={async (name, description) => {
                        await handleUpdate(name, description);

                        setShowEditDialog(false);
                    }}
                />
            )}
        </div>
    );
};

export default AiSkillDetail;
