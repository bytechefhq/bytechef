import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Input} from '@/components/ui/input';
import AssetFileDetailSheet from '@/pages/automation/asset-files/components/AssetFileDetailSheet';
import AssetFileList from '@/pages/automation/asset-files/components/AssetFileList';
import AssetFileUploadZone from '@/pages/automation/asset-files/components/AssetFileUploadZone';
import {useAssetFileUpload} from '@/pages/automation/asset-files/hooks/useAssetFileUpload';
import {useAssetFilesStore} from '@/pages/automation/asset-files/stores/useAssetFilesStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
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

    const {data: tagsData, isLoading: tagsIsLoading} = useGetAssetFileTagsQuery({workspaceId: workspaceIdAsString});

    const files = useMemo(() => filesData?.assetFiles ?? [], [filesData?.assetFiles]);
    const tags = useMemo(() => tagsData?.assetFileTags ?? [], [tagsData?.assetFileTags]);

    const totalFileCount = tagId ? (unfilteredFilesData?.assetFiles?.length ?? files.length) : files.length;

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

            <AlertDialog
                onCancel={() => setFileIdToDelete(null)}
                onConfirm={handleDeleteConfirm}
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
                    <DialogMain>
                        <DialogHeader description="Enter a new name for this file." title="Rename File" />

                        <DialogBody>
                            <Input
                                autoFocus
                                onChange={(event) => setRenameValue(event.target.value)}
                                placeholder="Enter new file name"
                                value={renameValue}
                            />
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button disabled={!renameValue.trim()} onClick={handleRenameSubmit}>
                                Rename
                            </Button>
                        </DialogFooter>
                    </DialogMain>
                </DialogContent>
            </Dialog>
        </>
    );

    const showCenteredEmpty = !isLoading && !error && totalFileCount === 0;

    const toolbarRight = (
        <div className="flex items-center gap-1">
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
                            <p className="px-2 text-xs">No tags.</p>
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
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );

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
