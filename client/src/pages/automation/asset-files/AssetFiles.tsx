import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import AssetFileDetailSheet from '@/pages/automation/asset-files/components/AssetFileDetailSheet';
import AssetFileList from '@/pages/automation/asset-files/components/AssetFileList';
import AssetFileUploadZone from '@/pages/automation/asset-files/components/AssetFileUploadZone';
import {useAssetFileUpload} from '@/pages/automation/asset-files/hooks/useAssetFileUpload';
import {useAssetFilesStore} from '@/pages/automation/asset-files/stores/useAssetFilesStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useOnEnvironmentChange} from '@/shared/hooks/useOnEnvironmentChange';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {
    useDeleteAssetFileMutation,
    useGetAssetFileTagsQuery,
    useGetAssetFilesQuery,
    useUpdateAssetFileMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {FileTextIcon, TagIcon, UploadIcon} from 'lucide-react';
import {ChangeEvent, ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import {useParams, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

const AssetFiles = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const tagId = searchParams.get('tagId');
    const {fileId: deepLinkFileId} = useParams<{fileId?: string}>();

    const [fileIdToDelete, setFileIdToDelete] = useState<string | null>(null);
    const [fileIdToRename, setFileIdToRename] = useState<string | null>(null);
    const [renameValue, setRenameValue] = useState<string>('');

    const fileInputRef = useRef<HTMLInputElement>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const searchQuery = useAssetFilesStore((state) => state.searchQuery);
    const setSearchQuery = useAssetFilesStore((state) => state.setSearchQuery);
    const setSelectedFileId = useAssetFilesStore((state) => state.setSelectedFileId);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    /*
     * Deep-link: when the route is `/automation/asset-files/:fileId`, pre-select that file so the
     * existing detail sheet opens automatically. Wraps the conversion in a parse guard so a stray
     * non-numeric path segment (link rot, typo) doesn't drop the user into a half-loaded sheet —
     * `setSelectedFileId(NaN)` would mount the sheet against a query for `assetFile(id="NaN")`,
     * which the API rejects after the round-trip and the user just sees a spinner-then-error. Bailing
     * before `setSelectedFileId` keeps the page in its normal list view in that edge case.
     */
    useEffect(() => {
        if (!deepLinkFileId) {
            return;
        }

        const parsed = Number(deepLinkFileId);

        if (!Number.isFinite(parsed)) {
            return;
        }

        setSelectedFileId(parsed);
    }, [deepLinkFileId, setSelectedFileId]);

    /*
     * On environment change, clear the in-page filter state (search query and ?tagId= URL param). The
     * file list itself refetches automatically because `currentEnvironmentId` is part of the React Query
     * key, but `searchQuery` and `tagId` are *UI* state that would otherwise persist across the env
     * switch and silently filter the new env's listing against irrelevant inputs (e.g. a tag id that
     * doesn't exist in the new env, leaving the list empty for non-obvious reasons). Mirrors the pattern
     * in WorkflowExecutions.
     */
    useOnEnvironmentChange(() => {
        setSearchQuery('');

        if (tagId) {
            const next = new URLSearchParams(searchParams);

            next.delete('tagId');
            setSearchParams(next, {replace: true});
        }
    });

    const {upload} = useAssetFileUpload();
    const queryClient = useQueryClient();

    const workspaceIdAsString = String(currentWorkspaceId);

    const selectedTagIds = tagId ? [tagId] : undefined;

    const {
        data: filesData,
        error,
        isLoading,
        refetch,
    } = useGetAssetFilesQuery({
        environment: currentEnvironmentId,
        mimeTypePrefix: null,
        tagIds: selectedTagIds,
        workspaceId: workspaceIdAsString,
    });

    const {data: unfilteredFilesData} = useGetAssetFilesQuery(
        {
            environment: currentEnvironmentId,
            mimeTypePrefix: null,
            workspaceId: workspaceIdAsString,
        },
        {enabled: !!tagId}
    );

    const {data: tagsData} = useGetAssetFileTagsQuery({workspaceId: workspaceIdAsString});

    const files = useMemo(() => filesData?.assetFiles ?? [], [filesData?.assetFiles]);
    const tags = useMemo(() => tagsData?.assetFileTags ?? [], [tagsData?.assetFileTags]);

    const totalFileCount = tagId ? (unfilteredFilesData?.assetFiles?.length ?? files.length) : files.length;

    // Source filter (sidebar tab) is URL-driven via `?source=AI_GENERATED|USER_UPLOAD` so a deep link to "the
    // Generated tab" round-trips and the back-button takes the user back to All Files. We deliberately do NOT
    // pass source to the GraphQL query; the asset_file index is small enough that an extra in-memory filter is
    // cheaper than another full query, and it lets us show "showing 0 of 27" even when the active tab is empty
    // — which is the kind of feedback that prevents "did my upload work?" support tickets.
    const sourceFilter = searchParams.get('source');

    const filteredFiles = useMemo(() => {
        let result = files;

        if (sourceFilter === 'AI_GENERATED' || sourceFilter === 'USER_UPLOAD') {
            result = result.filter((file) => file.source === sourceFilter);
        }

        const normalized = searchQuery.trim().toLowerCase();

        if (!normalized) {
            return result;
        }

        return result.filter((file) => file.name.toLowerCase().includes(normalized));
    }, [files, searchQuery, sourceFilter]);

    const deleteMutation = useDeleteAssetFileMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});

            setFileIdToDelete(null);
        },
    });

    const updateMutation = useUpdateAssetFileMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});

            setFileIdToRename(null);
            setRenameValue('');
        },
    });

    const handleUploadClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileInputChange = async (event: ChangeEvent<HTMLInputElement>) => {
        const selectedFiles = Array.from(event.target.files ?? []);

        for (const file of selectedFiles) {
            try {
                await upload(currentWorkspaceId, currentEnvironmentId, file);
            } catch (uploadError) {
                console.error('Upload failed', uploadError);

                toast.error(`Upload failed: ${file.name}`);
            }
        }

        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }

        void refetch();
    };

    const handleRenameOpen = (fileId: string, fileName: string) => {
        setFileIdToRename(fileId);
        setRenameValue(fileName);
    };

    const handleRenameSubmit = () => {
        if (!fileIdToRename) {
            return;
        }

        const trimmed = renameValue.trim();

        if (!trimmed) {
            return;
        }

        updateMutation.mutate({input: {id: fileIdToRename, name: trimmed}});
    };

    const handleDeleteOpen = (fileId: string) => {
        setFileIdToDelete(fileId);
    };

    const handleDeleteConfirm = () => {
        if (!fileIdToDelete) {
            return;
        }

        deleteMutation.mutate({id: fileIdToDelete});
    };

    const hiddenFileInput = (
        <input
            className="hidden"
            data-testid="asset-file-input"
            multiple
            onChange={handleFileInputChange}
            ref={fileInputRef}
            type="file"
        />
    );

    const dialogs: ReactNode = (
        <>
            <AssetFileDetailSheet />

            <DeleteAlertDialog
                onCancel={() => setFileIdToDelete(null)}
                onDelete={handleDeleteConfirm}
                open={fileIdToDelete !== null}
            />

            <Dialog
                onOpenChange={(open) => {
                    if (!open) {
                        setFileIdToRename(null);
                        setRenameValue('');
                    }
                }}
                open={fileIdToRename !== null}
            >
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Rename File</DialogTitle>

                        <DialogDescription>Enter a new name for this file.</DialogDescription>
                    </DialogHeader>

                    <Input
                        autoFocus
                        onChange={(event) => setRenameValue(event.target.value)}
                        placeholder="Enter new file name"
                        value={renameValue}
                    />

                    <DialogFooter>
                        <Button
                            onClick={() => {
                                setFileIdToRename(null);
                                setRenameValue('');
                            }}
                            variant="outline"
                        >
                            Cancel
                        </Button>

                        <Button disabled={!renameValue.trim()} onClick={handleRenameSubmit}>
                            Rename
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );

    const showCenteredEmpty = !isLoading && !error && totalFileCount === 0;

    // Search + Upload affordances hide when the page has zero files: the centered empty state already has
    // its own Upload button, and a Search input over an empty list reads as broken UX. EnvironmentSelect
    // stays visible regardless so the user can still flip envs from an empty workspace.
    const toolbarRight = (
        <div className="flex items-center gap-1">
            <CopilotButton source={Source.ASSET_FILE} />

            {!showCenteredEmpty && (
                <>
                    <Input
                        className="w-64"
                        data-testid="asset-file-search"
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Search files..."
                        value={searchQuery}
                    />

                    <Button icon={<UploadIcon />} onClick={handleUploadClick}>
                        Upload
                    </Button>
                </>
            )}
        </div>
    );

    const sidebarBody = (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem item={{current: !sourceFilter && !tagId, name: 'All Files'}} toLink="" />

                        <LeftSidebarNavItem
                            item={{current: sourceFilter === 'USER_UPLOAD', name: 'Uploaded'}}
                            toLink="?source=USER_UPLOAD"
                        />

                        <LeftSidebarNavItem
                            item={{current: sourceFilter === 'AI_GENERATED', name: 'Generated'}}
                            toLink="?source=AI_GENERATED"
                        />
                    </>
                }
                title="Source"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!tags.length ? (
                            <p className="px-3 text-xs">No tags.</p>
                        ) : (
                            tags.map((tag) => (
                                <LeftSidebarNavItem
                                    icon={<TagIcon className="mr-1 size-4" />}
                                    item={{
                                        current: tagId === tag.id,
                                        id: tag.id,
                                        name: tag.name,
                                    }}
                                    key={tag.id}
                                    toLink={`?tagId=${tag.id}`}
                                />
                            ))
                        )}
                    </>
                }
                title="Tags"
            />
        </>
    );

    /*
     * Refresh the file list after a BUILD-mode copilot turn creates, downloads, or edits a file, so the
     * page reflects it without a manual reload. Prefix-only key: the listing query is keyed by
     * {environment, mimeTypePrefix, tagIds, workspaceId}, and a copilot turn can change any of those
     * dimensions, so invalidating the bare prefix is the correct breadth here.
     *
     * Also invalidate the detail-sheet queries (GetAssetFile, GetAssetFileTextContent,
     * GetAssetFileVersions): updateAssetFileContent is a BUILD-mode tool that rewrites a file's content
     * and appends a version row, and if the detail sheet for that file happens to be open, its three
     * id-keyed queries would otherwise keep showing stale content and a version list missing the new
     * entry until the user closes and reopens it. Bare prefix here too, for the same reason as the
     * listing — the turn can touch a file other than the one currently open, and invalidating an
     * unmounted detail query costs nothing. Deliberately NOT invalidating GetAssetFileTags: none of the
     * seven asset-file tools creates, renames, deletes, or reassigns a tag.
     */
    useEffect(() => {
        return registerPostTurn(Source.ASSET_FILE, () => {
            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFile']});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFileTextContent']});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFileVersions']});
        });
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" right={toolbarRight} title="Files" />}
            leftSidebarBody={sidebarBody}
            leftSidebarHeader={<Header position="sidebar" title="Files" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                <AssetFileUploadZone
                    environment={currentEnvironmentId}
                    onUploaded={() => void refetch()}
                    workspaceId={currentWorkspaceId}
                >
                    {/*
                     * Three-way render: zero-files empty state, filtered-empty state, or the list. All three
                     * branches now live INSIDE LayoutContainer so the left sidebar (filter nav) and the
                     * main header (with the environment selector) render on every state. Earlier the
                     * zero-files case took an early-return path that bypassed the LayoutContainer chrome —
                     * the user landed on a blank canvas with just an Upload button, no environment
                     * selector, no sidebar — looking like a broken page rather than an intentional empty.
                     */}

                    {showCenteredEmpty ? (
                        <div className="flex size-full items-center justify-center">
                            <EmptyList
                                button={
                                    <Button icon={<UploadIcon />} onClick={handleUploadClick}>
                                        Upload a File
                                    </Button>
                                }
                                icon={<FileTextIcon className="size-24 text-stroke-neutral-tertiary" />}
                                message="Upload a file or drop one here to get started."
                                title="No Files"
                            />
                        </div>
                    ) : filteredFiles.length > 0 ? (
                        <AssetFileList
                            files={filteredFiles}
                            onDelete={(fileId) => handleDeleteOpen(fileId)}
                            onRename={handleRenameOpen}
                            tags={tags}
                        />
                    ) : (
                        <div className="flex size-full items-center justify-center">
                            <EmptyList
                                icon={<FileTextIcon className="size-24 text-stroke-neutral-tertiary" />}
                                message="No files match the current filters."
                                title="No Matching Files"
                            />
                        </div>
                    )}
                </AssetFileUploadZone>
            </PageLoader>

            {hiddenFileInput}

            {dialogs}
        </LayoutContainer>
    );
};

export default AssetFiles;
