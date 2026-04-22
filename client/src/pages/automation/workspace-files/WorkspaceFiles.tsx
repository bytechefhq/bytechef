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
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkspaceFileDetailSheet from '@/pages/automation/workspace-files/components/WorkspaceFileDetailSheet';
import WorkspaceFileRow from '@/pages/automation/workspace-files/components/WorkspaceFileRow';
import WorkspaceFileUploadZone from '@/pages/automation/workspace-files/components/WorkspaceFileUploadZone';
import {useWorkspaceFileUpload} from '@/pages/automation/workspace-files/hooks/useWorkspaceFileUpload';
import {useWorkspaceFilesStore} from '@/pages/automation/workspace-files/stores/useWorkspaceFilesStore';
import {openCopilotForFiles} from '@/shared/components/copilot/stores/useCopilotPanelStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {
    useDeleteWorkspaceFileMutation,
    useGetWorkspaceFileTagsQuery,
    useGetWorkspaceFilesQuery,
    useUpdateWorkspaceFileMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FileTextIcon, SparklesIcon, TagIcon, UploadIcon} from 'lucide-react';
import {ChangeEvent, ReactNode, useMemo, useRef, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

const WorkspaceFiles = () => {
    const [searchParams] = useSearchParams();
    const tagId = searchParams.get('tagId');

    const [fileIdToDelete, setFileIdToDelete] = useState<string | null>(null);
    const [fileIdToRename, setFileIdToRename] = useState<string | null>(null);
    const [renameValue, setRenameValue] = useState<string>('');

    const fileInputRef = useRef<HTMLInputElement>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const searchQuery = useWorkspaceFilesStore((state) => state.searchQuery);
    const setSearchQuery = useWorkspaceFilesStore((state) => state.setSearchQuery);

    const {upload} = useWorkspaceFileUpload();
    const queryClient = useQueryClient();

    const workspaceIdAsString = String(currentWorkspaceId);

    const selectedTagIds = tagId ? [tagId] : undefined;

    const {
        data: filesData,
        error,
        isLoading,
        refetch,
    } = useGetWorkspaceFilesQuery({
        mimeTypePrefix: null,
        tagIds: selectedTagIds,
        workspaceId: workspaceIdAsString,
    });

    const {data: unfilteredFilesData} = useGetWorkspaceFilesQuery(
        {
            mimeTypePrefix: null,
            workspaceId: workspaceIdAsString,
        },
        {enabled: !!tagId}
    );

    const {data: tagsData} = useGetWorkspaceFileTagsQuery({workspaceId: workspaceIdAsString});

    const files = useMemo(() => filesData?.workspaceFiles ?? [], [filesData?.workspaceFiles]);
    const tags = useMemo(() => tagsData?.workspaceFileTags ?? [], [tagsData?.workspaceFileTags]);

    const totalFileCount = tagId ? (unfilteredFilesData?.workspaceFiles?.length ?? files.length) : files.length;

    const filteredFiles = useMemo(() => {
        const normalized = searchQuery.trim().toLowerCase();

        if (!normalized) {
            return files;
        }

        return files.filter((file) => file.name.toLowerCase().includes(normalized));
    }, [files, searchQuery]);

    const deleteMutation = useDeleteWorkspaceFileMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFiles']});

            setFileIdToDelete(null);
        },
    });

    const updateMutation = useUpdateWorkspaceFileMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFiles']});

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
                await upload(currentWorkspaceId, file);
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

    const handleCreateWithAiClick = () => {
        openCopilotForFiles();
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
            data-testid="workspace-file-input"
            multiple
            onChange={handleFileInputChange}
            ref={fileInputRef}
            type="file"
        />
    );

    const dialogs: ReactNode = (
        <>
            <WorkspaceFileDetailSheet />

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

    if (showCenteredEmpty) {
        return (
            <WorkspaceFileUploadZone onUploaded={() => void refetch()} workspaceId={currentWorkspaceId}>
                <div className="flex size-full items-center justify-center">
                    <EmptyList
                        button={
                            <Button icon={<UploadIcon />} onClick={handleUploadClick}>
                                Upload a File
                            </Button>
                        }
                        icon={<FileTextIcon className="size-24 text-gray-300" />}
                        message="Upload a file or drop one here to get started."
                        title="No Files"
                    />
                </div>

                {hiddenFileInput}

                {dialogs}
            </WorkspaceFileUploadZone>
        );
    }

    const toolbarRight = (
        <div className="flex items-center gap-2">
            <Input
                className="w-64"
                data-testid="workspace-file-search"
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Search files..."
                value={searchQuery}
            />

            <Button icon={<UploadIcon />} onClick={handleUploadClick}>
                Upload
            </Button>

            <Button
                icon={<SparklesIcon />}
                onClick={handleCreateWithAiClick}
                size="iconSm"
                title="Create with AI"
                variant="outline"
            />
        </div>
    );

    const sidebarBody = (
        <LeftSidebarNav
            body={
                <>
                    <LeftSidebarNavItem item={{current: !tagId, name: 'All Files'}} toLink="" />

                    {tags.map((tag) => (
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
                    ))}
                </>
            }
            title="Tags"
        />
    );

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" right={toolbarRight} title="Files" />}
            leftSidebarBody={sidebarBody}
            leftSidebarHeader={<Header position="sidebar" title="Files" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                <WorkspaceFileUploadZone onUploaded={() => void refetch()} workspaceId={currentWorkspaceId}>
                    {filteredFiles.length > 0 ? (
                        <div className="w-full divide-y divide-border/50 px-4">
                            {filteredFiles.map((file) => (
                                <WorkspaceFileRow
                                    file={file}
                                    key={file.id}
                                    onDelete={(fileId) => handleDeleteOpen(fileId)}
                                    onRename={handleRenameOpen}
                                />
                            ))}
                        </div>
                    ) : (
                        <EmptyList
                            icon={<FileTextIcon className="size-24 text-gray-300" />}
                            message="No files match the current filters."
                            title="No Matching Files"
                        />
                    )}
                </WorkspaceFileUploadZone>

                {hiddenFileInput}

                {dialogs}
            </PageLoader>
        </LayoutContainer>
    );
};

export default WorkspaceFiles;
