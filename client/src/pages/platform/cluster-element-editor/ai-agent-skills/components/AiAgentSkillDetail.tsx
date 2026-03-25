import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import useAgentSkillDetail, {
    type FileTreeNodeI,
} from '@/pages/platform/cluster-element-editor/ai-agent-skills/hooks/useAgentSkillDetail';
import {EditorContent, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {DownloadIcon, FileIcon, FileTextIcon, FolderIcon} from 'lucide-react';
import {Suspense, lazy, useEffect} from 'react';
import {twMerge} from 'tailwind-merge';
import {Markdown} from 'tiptap-markdown';

const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

function FileTreeNode({
    node,
    onSelect,
    selectedPath,
}: {
    node: FileTreeNodeI;
    onSelect: (path: string) => void;
    selectedPath: string | null;
}) {
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
        <button
            className={twMerge(
                'flex w-full items-center gap-1.5 rounded px-2 py-1 text-left text-sm hover:bg-gray-100',
                isSelected && 'bg-blue-50 text-blue-700'
            )}
            onClick={() => onSelect(node.path)}
        >
            {node.name.toLowerCase().endsWith('.md') ? (
                <FileTextIcon className="size-4" />
            ) : (
                <FileIcon className="size-4" />
            )}

            <span>{node.name}</span>
        </button>
    );
}

function parseFrontmatter(content: string): {body: string; frontmatter: Record<string, string> | null} {
    const frontmatterMatch = content.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);

    if (!frontmatterMatch) {
        return {body: content, frontmatter: null};
    }

    const frontmatterLines = frontmatterMatch[1].split('\n');
    const frontmatter: Record<string, string> = {};

    for (const line of frontmatterLines) {
        const colonIndex = line.indexOf(':');

        if (colonIndex > 0) {
            const key = line.substring(0, colonIndex).trim();
            const value = line.substring(colonIndex + 1).trim();

            frontmatter[key] = value;
        }
    }

    return {body: frontmatterMatch[2].trim(), frontmatter};
}

function FrontmatterTable({frontmatter}: {frontmatter: Record<string, string>}) {
    return (
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
}

function MarkdownViewer({content}: {content: string}) {
    const {body, frontmatter} = parseFrontmatter(content);

    const editor = useEditor({
        content: body,
        editable: false,
        extensions: [StarterKit, Markdown],
    });

    useEffect(() => {
        if (editor && body) {
            editor.commands.setContent(body);
        }
    }, [body, editor]);

    return (
        <div className="overflow-y-auto p-6">
            {frontmatter && <FrontmatterTable frontmatter={frontmatter} />}

            <div className="prose prose-sm max-w-none">
                <EditorContent editor={editor} />
            </div>
        </div>
    );
}

const AiAgentSkillDetail = () => {
    const {
        editorLanguage,
        fileContent,
        fileTree,
        handleDownload,
        handleFileSelect,
        isFileContentLoading,
        isMarkdown,
        selectedFilePath,
        skill,
    } = useAgentSkillDetail();

    if (!skill) {
        return (
            <div className="flex flex-1 items-center justify-center">
                <LoadingIcon />
            </div>
        );
    }

    return (
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
                    <>
                        <div className="flex shrink-0 items-center border-b border-b-border/50 py-2 pl-4 pr-0">
                            <span className="flex-1 text-center text-sm text-gray-500">{selectedFilePath}</span>

                            <Button
                                icon={<DownloadIcon className="size-4" />}
                                onClick={handleDownload}
                                size="icon"
                                variant="ghost"
                            />
                        </div>

                        <div className="relative min-h-0 flex-1">
                            {isFileContentLoading ? (
                                <div className="flex items-center justify-center p-8">
                                    <LoadingIcon />
                                </div>
                            ) : isMarkdown ? (
                                <div className="absolute inset-0 overflow-y-auto">
                                    <MarkdownViewer content={fileContent} />
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
                                            onChange={() => {}}
                                            onMount={() => {}}
                                            options={{
                                                automaticLayout: true,
                                                folding: true,
                                                lineNumbers: 'on',
                                                minimap: {enabled: false},
                                                readOnly: true,
                                                scrollBeyondLastLine: false,
                                                wordWrap: 'on',
                                            }}
                                            value={fileContent}
                                        />
                                    </Suspense>
                                </div>
                            )}
                        </div>
                    </>
                ) : (
                    <div className="flex flex-1 items-center justify-center text-sm text-gray-400">
                        Select a file to view its contents
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiAgentSkillDetail;
