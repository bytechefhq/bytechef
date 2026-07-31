import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {useAssetFilesStore} from '@/pages/automation/asset-files/stores/useAssetFilesStore';
import AssetFileViewer, {type AssetFileViewerModeType} from '@/shared/components/asset-file-viewer/AssetFileViewer';
import {
    useGetAssetFileQuery,
    useGetAssetFileTextContentQuery,
    useGetAssetFileVersionsQuery,
    useRestoreAssetFileVersionMutation,
    useUpdateAssetFileTextContentMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {FileTextIcon, HistoryIcon, SaveIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';

/**
 * Resolves the {@code environmentId} ordinal stored on an {@code AssetFile} to a short label suitable for the badge in
 * the detail-sheet header. Mirrors the {@code Environment} enum's ordinal pinning on the server (DEVELOPMENT=0,
 * STAGING=1, PRODUCTION=2). The fallback "ENV {n}" guards against a future server-side append the client hasn't been
 * rebuilt against, so a brand-new ordinal renders as e.g. {@code "ENV 3"} instead of crashing the sheet.
 */
const environmentLabel = (environmentId: number): string => {
    switch (environmentId) {
        case 0:
            return 'DEV';
        case 1:
            return 'STAGING';
        case 2:
            return 'PROD';
        default:
            return `ENV ${environmentId}`;
    }
};

const isTextMime = (mimeType: string): boolean => mimeType.startsWith('text/') || mimeType === 'application/json';

/**
 * True when the shared viewer has a rendered (non-editor) representation for the file — either through the
 * format column (AI-generated artifacts) or through mime/extension sniffing. Drives the default view mode:
 * renderable files open in Preview, plain text/code files open straight in the editor.
 */
const hasRenderedPreview = (fileName: string, mimeType: string, format: string | null | undefined): boolean => {
    if (format === 'CHART' || format === 'CSV' || format === 'HTML' || format === 'MARKDOWN') {
        return true;
    }

    if (mimeType === 'text/markdown' || mimeType === 'text/csv' || mimeType === 'text/html') {
        return true;
    }

    return (
        fileName.endsWith('.md') ||
        fileName.endsWith('.markdown') ||
        fileName.endsWith('.csv') ||
        fileName.endsWith('.html') ||
        fileName.endsWith('.htm')
    );
};

const formatBytes = (bytes: number): string => {
    if (!bytes) {
        return '0 B';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    const value = bytes / Math.pow(1024, exponent);

    return `${value.toFixed(value >= 10 || exponent === 0 ? 0 : 1)} ${units[exponent]}`;
};

const formatDate = (value: number | string | null | undefined): string => {
    if (value == null) {
        return '';
    }

    const millis = typeof value === 'number' ? value : Number(value);

    if (Number.isNaN(millis)) {
        return '';
    }

    return new Date(millis).toLocaleString();
};

const AssetFileDetailSheet = () => {
    const [editorValue, setEditorValue] = useState<string>('');
    const [showVersions, setShowVersions] = useState(false);
    const [viewMode, setViewMode] = useState<AssetFileViewerModeType>('preview');

    const selectedFileId = useAssetFilesStore((state) => state.selectedFileId);
    const setSelectedFileId = useAssetFilesStore((state) => state.setSelectedFileId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const fileIdAsString = selectedFileId != null ? String(selectedFileId) : '';
    const enabled = selectedFileId != null;

    const {data: fileData} = useGetAssetFileQuery({id: fileIdAsString}, {enabled});

    const file = fileData?.assetFile ?? null;

    const isText = file ? isTextMime(file.mimeType) : false;
    const isRenderable = file ? hasRenderedPreview(file.name, file.mimeType, file.format) : false;

    const {data: textContentData} = useGetAssetFileTextContentQuery({id: fileIdAsString}, {enabled: enabled && isText});

    const {data: versionsData} = useGetAssetFileVersionsQuery({id: fileIdAsString}, {enabled: enabled && showVersions});

    const invalidateFileQueries = () => {
        void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});
        void queryClient.invalidateQueries({queryKey: ['GetAssetFile', {id: fileIdAsString}]});
        void queryClient.invalidateQueries({queryKey: ['GetAssetFileTextContent', {id: fileIdAsString}]});
        void queryClient.invalidateQueries({queryKey: ['GetAssetFileVersions', {id: fileIdAsString}]});
    };

    const updateTextContentMutation = useUpdateAssetFileTextContentMutation({
        onSuccess: () => {
            invalidateFileQueries();

            toast.success('File saved');
        },
    });

    const restoreVersionMutation = useRestoreAssetFileVersionMutation({
        onSuccess: () => {
            invalidateFileQueries();

            toast.success('Version restored');
        },
    });

    const versions = versionsData?.assetFileVersions ?? [];

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            setSelectedFileId(null);
            setEditorValue('');
            setShowVersions(false);
            setViewMode('preview');
        }
    };

    const handleSaveClick = () => {
        if (!file) {
            return;
        }

        updateTextContentMutation.mutate({content: editorValue, id: file.id});
    };

    const handleRestoreVersionClick = (versionId: string) => {
        if (!file) {
            return;
        }

        restoreVersionMutation.mutate({id: file.id, versionId});
    };

    useEffect(() => {
        if (textContentData?.assetFileTextContent != null) {
            setEditorValue(textContentData.assetFileTextContent);
        }
    }, [textContentData?.assetFileTextContent]);

    useEffect(() => {
        setShowVersions(false);
    }, [selectedFileId]);

    // Plain text/code files open straight in the editor (there is nothing to render), everything with a
    // rendered representation opens in Preview. Runs when the file row loads because the decision needs the
    // file's mime/format, which arrive async.
    useEffect(() => {
        if (!file) {
            return;
        }

        setViewMode(isText && !isRenderable ? 'editor' : 'preview');
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [file?.id]);

    return (
        <Sheet onOpenChange={handleOpenChange} open={selectedFileId !== null}>
            <VisuallyHidden.Root>
                <SheetTitle>{file?.name ?? 'File'}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto w-[90%] flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[900px]"
                data-testid="asset-file-detail-sheet"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                {file ? (
                    <>
                        <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                            <div className="flex items-center gap-x-2">
                                <FileTextIcon className="size-5 text-content-neutral-secondary" />

                                <span className="flex flex-col">
                                    <span className="flex items-center gap-x-2">
                                        <strong className="text-base text-content-neutral-primary">{file.name}</strong>

                                        {/*
                                         * Env badge: makes the file's environment visible at a glance, and the
                                         * destructive style makes a deep-linked file from a foreign environment
                                         * obvious without forcing the user to read the body hint below. The label
                                         * uses short forms (DEV/STAGING/PROD) to fit the header without truncation.
                                         */}

                                        <Badge
                                            data-testid="asset-file-environment-badge"
                                            label={environmentLabel(Number(file.environmentId))}
                                            styleType={
                                                Number(file.environmentId) === currentEnvironmentId
                                                    ? 'secondary-outline'
                                                    : 'destructive-outline'
                                            }
                                        />
                                    </span>

                                    <span className="text-xs text-content-neutral-secondary">
                                        {file.mimeType} &middot; {file.source}
                                    </span>
                                </span>
                            </div>

                            <div className="flex items-center gap-x-2">
                                {isText && (
                                    <div className="flex items-center rounded-md border border-border/50">
                                        <Button
                                            data-testid="asset-file-preview-toggle"
                                            label="Preview"
                                            onClick={() => setViewMode('preview')}
                                            size="sm"
                                            variant={viewMode === 'preview' ? 'secondary' : 'ghost'}
                                        />

                                        <Button
                                            data-testid="asset-file-split-toggle"
                                            label="Split"
                                            onClick={() => setViewMode('split')}
                                            size="sm"
                                            variant={viewMode === 'split' ? 'secondary' : 'ghost'}
                                        />

                                        <Button
                                            data-testid="asset-file-edit-toggle"
                                            label="Edit"
                                            onClick={() => setViewMode('editor')}
                                            size="sm"
                                            variant={viewMode === 'editor' ? 'secondary' : 'ghost'}
                                        />
                                    </div>
                                )}

                                <Button
                                    data-testid="asset-file-history-toggle"
                                    icon={<HistoryIcon />}
                                    onClick={() => setShowVersions((previousShowVersions) => !previousShowVersions)}
                                    size="iconSm"
                                    title="Version history"
                                    variant={showVersions ? 'secondary' : 'ghost'}
                                />

                                {isText && (
                                    <Button
                                        disabled={updateTextContentMutation.isPending}
                                        icon={<SaveIcon />}
                                        onClick={handleSaveClick}
                                        size="iconSm"
                                        title="Save"
                                    />
                                )}

                                <SheetCloseButton />
                            </div>
                        </header>

                        {Number(file.environmentId) !== currentEnvironmentId && (
                            // Cross-env deep-link explanation. Without this banner a user who clicks a deep-link
                            // (e.g. from an audit log) into a file from another environment sees the detail sheet but
                            // an empty list behind it, with no obvious reason — the list is correctly filtering by the
                            // current env while the deep-link forced the sheet open. The hint explains the mismatch
                            // and leaves env-switching to the existing EnvironmentSelect on the page header rather
                            // than auto-mutating the user's selection.
                            <div
                                className="shrink-0 border-b border-b-border/50 bg-amber-50 p-3 text-xs text-amber-900"
                                data-testid="asset-file-env-mismatch"
                            >
                                {`This file lives in ${environmentLabel(Number(file.environmentId))}, but you are currently viewing the ${environmentLabel(currentEnvironmentId)} file list. Switch environments via the toolbar selector to see it alongside its peers.`}
                            </div>
                        )}

                        {showVersions && (
                            <div
                                className="max-h-48 shrink-0 overflow-auto border-b border-b-border/50 bg-surface-neutral-primary"
                                data-testid="asset-file-versions"
                            >
                                {versions.length === 0 ? (
                                    <p className="p-3 text-xs text-muted-foreground">
                                        No previous versions. A version is captured every time the file&apos;s content
                                        changes.
                                    </p>
                                ) : (
                                    <ul>
                                        {versions.map((version) => (
                                            <li
                                                className="flex items-center justify-between border-b border-b-border/30 px-3 py-2 text-xs"
                                                data-testid={`asset-file-version-${version.id}`}
                                                key={version.id}
                                            >
                                                <span className="flex items-center gap-3">
                                                    <span className="font-semibold">v{version.versionNumber}</span>

                                                    <span className="text-muted-foreground">
                                                        {formatBytes(Number(version.sizeBytes))}
                                                    </span>

                                                    <span className="text-muted-foreground">
                                                        {formatDate(version.createdDate)}
                                                    </span>

                                                    {version.createdBy && (
                                                        <span className="text-muted-foreground">
                                                            {version.createdBy}
                                                        </span>
                                                    )}
                                                </span>

                                                <Button
                                                    disabled={restoreVersionMutation.isPending}
                                                    label="Restore"
                                                    onClick={() => handleRestoreVersionClick(version.id)}
                                                    size="sm"
                                                    variant="outline"
                                                />
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        )}

                        <div className="flex min-h-0 flex-1 p-3">
                            <div className="flex min-w-0 flex-1 flex-col overflow-hidden rounded-md bg-surface-neutral-primary">
                                <AssetFileViewer
                                    editorContent={isText ? editorValue : undefined}
                                    fileId={fileIdAsString}
                                    name={file.name}
                                    onEditorContentChange={isText ? setEditorValue : undefined}
                                    viewMode={viewMode}
                                />
                            </div>
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

export default AssetFileDetailSheet;
