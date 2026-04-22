import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {useWorkspaceFilesStore} from '@/pages/automation/workspace-files/stores/useWorkspaceFilesStore';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {openCopilotForFiles} from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {
    WorkspaceFileSource,
    useGetWorkspaceFileQuery,
    useGetWorkspaceFileTextContentQuery,
    useUpdateWorkspaceFileTagsMutation,
    useUpdateWorkspaceFileTextContentMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {DownloadIcon, SaveIcon, SparklesIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useMemo, useState} from 'react';
import {toast} from 'sonner';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const PROMPT_PREVIEW_LENGTH = 200;

const isTextMime = (mimeType: string): boolean => mimeType.startsWith('text/') || mimeType === 'application/json';

const isImageMime = (mimeType: string): boolean => mimeType.startsWith('image/');

const isPdfMime = (mimeType: string): boolean => mimeType === 'application/pdf';

const inferLanguage = (fileName: string, mimeType: string): string => {
    const extension = fileName.includes('.') ? fileName.split('.').pop()?.toLowerCase() : undefined;

    switch (extension) {
        case 'js':
        case 'mjs':
        case 'cjs':
            return 'javascript';
        case 'ts':
        case 'tsx':
            return 'typescript';
        case 'json':
            return 'json';
        case 'md':
        case 'markdown':
            return 'markdown';
        case 'py':
            return 'python';
        case 'rb':
            return 'ruby';
        case 'yml':
        case 'yaml':
            return 'yaml';
        case 'html':
        case 'htm':
            return 'html';
        case 'css':
            return 'css';
        case 'sql':
            return 'sql';
        case 'xml':
            return 'xml';
        case 'sh':
        case 'bash':
            return 'shell';
        default:
            if (mimeType === 'application/json') {
                return 'json';
            }

            if (mimeType === 'text/markdown') {
                return 'markdown';
            }

            return 'plaintext';
    }
};

const WorkspaceFileDetailSheet = () => {
    const [editorValue, setEditorValue] = useState<string>('');
    const [tagsInputValue, setTagsInputValue] = useState<string>('');
    const [showFullPrompt, setShowFullPrompt] = useState<boolean>(false);

    const selectedFileId = useWorkspaceFilesStore((state) => state.selectedFileId);
    const setSelectedFileId = useWorkspaceFilesStore((state) => state.setSelectedFileId);

    const queryClient = useQueryClient();

    const fileIdAsString = selectedFileId != null ? String(selectedFileId) : '';
    const enabled = selectedFileId != null;

    const {data: fileData} = useGetWorkspaceFileQuery({id: fileIdAsString}, {enabled});

    const file = fileData?.workspaceFile ?? null;

    const isText = file ? isTextMime(file.mimeType) : false;
    const isImage = file ? isImageMime(file.mimeType) : false;
    const isPdf = file ? isPdfMime(file.mimeType) : false;

    const {data: textContentData} = useGetWorkspaceFileTextContentQuery(
        {id: fileIdAsString},
        {enabled: enabled && isText}
    );

    const updateTextContentMutation = useUpdateWorkspaceFileTextContentMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFiles']});
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFile', {id: fileIdAsString}]});
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFileTextContent', {id: fileIdAsString}]});

            toast.success('File saved');
        },
    });

    const updateTagsMutation = useUpdateWorkspaceFileTagsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFiles']});
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFile', {id: fileIdAsString}]});

            toast.success('Tags updated');
        },
    });

    const language = useMemo(() => (file ? inferLanguage(file.name, file.mimeType) : 'plaintext'), [file]);

    const promptPreview = useMemo(() => {
        if (!file?.generatedFromPrompt) {
            return '';
        }

        if (showFullPrompt || file.generatedFromPrompt.length <= PROMPT_PREVIEW_LENGTH) {
            return file.generatedFromPrompt;
        }

        return `${file.generatedFromPrompt.slice(0, PROMPT_PREVIEW_LENGTH)}...`;
    }, [file, showFullPrompt]);

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            setSelectedFileId(null);
            setEditorValue('');
            setTagsInputValue('');
            setShowFullPrompt(false);
        }
    };

    const handleSaveClick = () => {
        if (!file) {
            return;
        }

        updateTextContentMutation.mutate({content: editorValue, id: file.id});
    };

    const handleTagsSave = () => {
        if (!file) {
            return;
        }

        const tagIds = tagsInputValue
            .split(',')
            .map((value) => value.trim())
            .filter((value) => value.length > 0);

        updateTagsMutation.mutate({id: file.id, tagIds});
    };

    const handleContinueInCopilotClick = () => {
        openCopilotForFiles();
    };

    useEffect(() => {
        if (textContentData?.workspaceFileTextContent != null) {
            setEditorValue(textContentData.workspaceFileTextContent);
        }
    }, [textContentData?.workspaceFileTextContent]);

    useEffect(() => {
        if (file) {
            setTagsInputValue(file.tags.map((tag) => tag.id).join(', '));
        }
    }, [file]);

    return (
        <Sheet onOpenChange={handleOpenChange} open={selectedFileId !== null}>
            <SheetContent
                className="flex h-full w-[90%] flex-col gap-0 p-0 sm:max-w-[900px]"
                data-testid="workspace-file-detail-sheet"
            >
                {file ? (
                    <>
                        <SheetHeader className="border-b p-6">
                            <SheetTitle>{file.name}</SheetTitle>

                            <SheetDescription>
                                {file.mimeType} &middot; {file.source}
                            </SheetDescription>
                        </SheetHeader>

                        <div className="flex min-h-0 flex-1">
                            <div className="flex min-w-0 flex-1 flex-col">
                                {isText && (
                                    <div className="flex flex-1 flex-col">
                                        <div className="flex items-center justify-end gap-2 border-b p-2">
                                            <Button
                                                disabled={updateTextContentMutation.isPending}
                                                icon={<SaveIcon />}
                                                onClick={handleSaveClick}
                                            >
                                                Save
                                            </Button>
                                        </div>

                                        <div className="flex-1" data-testid="workspace-file-monaco">
                                            <Suspense fallback={<MonacoEditorLoader />}>
                                                <MonacoEditor
                                                    defaultLanguage={language}
                                                    onChange={(value) => setEditorValue(value ?? '')}
                                                    onMount={() => {}}
                                                    options={{
                                                        automaticLayout: true,
                                                        fontSize: 12,
                                                        minimap: {enabled: false},
                                                        scrollBeyondLastLine: false,
                                                        wordWrap: 'on',
                                                    }}
                                                    value={editorValue}
                                                />
                                            </Suspense>
                                        </div>
                                    </div>
                                )}

                                {isImage && (
                                    <div className="flex flex-1 items-center justify-center overflow-auto p-4">
                                        <img
                                            alt={file.name}
                                            className="max-h-full max-w-full"
                                            data-testid="workspace-file-image"
                                            src={file.downloadUrl}
                                        />
                                    </div>
                                )}

                                {isPdf && (
                                    <iframe
                                        className="flex-1"
                                        data-testid="workspace-file-iframe"
                                        src={file.downloadUrl}
                                        title={file.name}
                                    />
                                )}

                                {!isText && !isImage && !isPdf && (
                                    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
                                        <p className="text-sm text-muted-foreground">Preview not available</p>

                                        <a
                                            className="inline-flex h-9 items-center justify-center gap-2 rounded-md bg-surface-brand-primary px-4 py-2 text-sm font-medium text-content-onsurface-primary hover:bg-surface-brand-primary-hover"
                                            data-testid="workspace-file-download"
                                            download={file.name}
                                            href={file.downloadUrl}
                                            rel="noreferrer"
                                        >
                                            <DownloadIcon className="size-4" /> Download
                                        </a>
                                    </div>
                                )}
                            </div>

                            <aside className="flex w-72 shrink-0 flex-col gap-4 border-l p-4">
                                <section>
                                    <h3 className="mb-2 text-sm font-semibold">Metadata</h3>

                                    <dl className="space-y-1 text-xs text-muted-foreground">
                                        <div className="flex justify-between gap-2">
                                            <dt>Size</dt>

                                            <dd>{String(file.sizeBytes)} B</dd>
                                        </div>

                                        <div className="flex justify-between gap-2">
                                            <dt>Created by</dt>

                                            <dd className="truncate">{file.createdBy || '—'}</dd>
                                        </div>

                                        <div className="flex justify-between gap-2">
                                            <dt>Last modified by</dt>

                                            <dd className="truncate">{file.lastModifiedBy || '—'}</dd>
                                        </div>
                                    </dl>
                                </section>

                                <section>
                                    <h3 className="mb-2 text-sm font-semibold">Tags</h3>

                                    <div className="mb-2 flex flex-wrap gap-1">
                                        {file.tags.length > 0 ? (
                                            file.tags.map((tag) => (
                                                <Badge
                                                    key={tag.id}
                                                    label={tag.name}
                                                    styleType="secondary-outline"
                                                    weight="regular"
                                                />
                                            ))
                                        ) : (
                                            <span className="text-xs text-muted-foreground">No tags</span>
                                        )}
                                    </div>

                                    <Input
                                        onChange={(event) => setTagsInputValue(event.target.value)}
                                        placeholder="Comma-separated tag IDs"
                                        value={tagsInputValue}
                                    />

                                    <Button
                                        className="mt-2 w-full"
                                        disabled={updateTagsMutation.isPending}
                                        onClick={handleTagsSave}
                                        variant="outline"
                                    >
                                        Update tags
                                    </Button>
                                </section>

                                {file.source === WorkspaceFileSource.AiGenerated && (
                                    <section className="rounded-md border bg-muted/30 p-3">
                                        <h3 className="mb-2 flex items-center gap-1 text-sm font-semibold">
                                            <SparklesIcon className="size-4" /> AI Provenance
                                        </h3>

                                        {file.generatedFromPrompt ? (
                                            <div className="space-y-2 text-xs">
                                                <p className="whitespace-pre-wrap">{promptPreview}</p>

                                                {file.generatedFromPrompt.length > PROMPT_PREVIEW_LENGTH && (
                                                    <button
                                                        className="text-xs text-primary underline"
                                                        onClick={() => setShowFullPrompt((previous) => !previous)}
                                                        type="button"
                                                    >
                                                        {showFullPrompt ? 'Show less' : 'Show more'}
                                                    </button>
                                                )}
                                            </div>
                                        ) : (
                                            <p className="text-xs text-muted-foreground">No prompt recorded.</p>
                                        )}

                                        <Button
                                            className="mt-3 w-full"
                                            icon={<SparklesIcon />}
                                            onClick={handleContinueInCopilotClick}
                                            variant="outline"
                                        >
                                            Continue in Copilot
                                        </Button>
                                    </section>
                                )}
                            </aside>
                        </div>
                    </>
                ) : (
                    <div className="flex size-full items-center justify-center">
                        <span className="text-sm text-muted-foreground">Loading...</span>
                    </div>
                )}
            </SheetContent>
        </Sheet>
    );
};

export default WorkspaceFileDetailSheet;
