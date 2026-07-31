import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useAssetFilesStore} from '@/pages/automation/asset-files/stores/useAssetFilesStore';
import TagList from '@/shared/components/TagList';
import {
    AssetFile,
    AssetFileSource,
    Tag,
    useDisableAssetFilePublicLinkMutation,
    useEnableAssetFilePublicLinkMutation,
    useUpdateAssetFileTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {
    DownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    FileIcon,
    FileImageIcon,
    FileTextIcon,
    Link2OffIcon,
    LinkIcon,
    SparklesIcon,
    Trash2Icon,
} from 'lucide-react';
import {MouseEvent} from 'react';
import {toast} from 'sonner';

interface AssetFileListItemPropsI {
    file: AssetFile;
    onDelete: (fileId: string, fileName: string) => void;
    onRename: (fileId: string, fileName: string) => void;
    remainingTags?: Tag[];
}

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

const pickMimeIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) {
        return <FileImageIcon className="size-5 text-muted-foreground" />;
    }

    if (mimeType.startsWith('text/') || mimeType === 'application/json') {
        return <FileTextIcon className="size-5 text-muted-foreground" />;
    }

    return <FileIcon className="size-5 text-muted-foreground" />;
};

const AssetFileListItem = ({file, onDelete, onRename, remainingTags}: AssetFileListItemPropsI) => {
    const setSelectedFileId = useAssetFilesStore((state) => state.setSelectedFileId);

    const queryClient = useQueryClient();

    const sizeLabel = formatBytes(Number(file.sizeBytes));
    const lastModifiedLabel = formatDate(file.lastModifiedDate);

    const updateTagsMutation = useUpdateAssetFileTagsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFile', {id: file.id}]});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFileTags']});
        },
    });

    const enablePublicLinkMutation = useEnableAssetFilePublicLinkMutation({
        onSuccess: (data) => {
            const publicLinkUrl = data.enableAssetFilePublicLink.publicLinkUrl;

            if (publicLinkUrl) {
                void navigator.clipboard.writeText(`${window.location.origin}${publicLinkUrl}`);

                toast.success('Public link enabled and copied to clipboard');
            }

            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFile', {id: file.id}]});
        },
    });

    const disablePublicLinkMutation = useDisableAssetFilePublicLinkMutation({
        onSuccess: () => {
            toast.success('Public link disabled');

            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});
            void queryClient.invalidateQueries({queryKey: ['GetAssetFile', {id: file.id}]});
        },
    });

    const handleCopyPublicLinkClick = () => {
        if (file.publicLinkUrl) {
            void navigator.clipboard.writeText(`${window.location.origin}${file.publicLinkUrl}`);

            toast.success('Public link copied to clipboard');
        }
    };

    const convertedTags = (file.tags ?? []).map((tag) => ({id: Number(tag.id), name: tag.name}));

    const convertedRemainingTags = (remainingTags ?? []).map((tag) => ({id: Number(tag.id), name: tag.name}));

    const handleRowClick = () => {
        setSelectedFileId(Number(file.id));
    };

    const handleStopPropagation = (event: MouseEvent) => {
        event.stopPropagation();
    };

    return (
        <div
            className="group mb-2 flex w-full cursor-pointer items-center justify-between rounded border border-border/50 px-3 hover:bg-destructive-foreground"
            data-testid={`asset-file-list-item-${file.id}`}
            onClick={handleRowClick}
        >
            <div className="flex flex-1 items-center py-3">
                <div className="flex flex-1 items-center gap-3">
                    {pickMimeIcon(file.mimeType)}

                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            <span className="text-base font-semibold">{file.name}</span>

                            {file.source === AssetFileSource.AiGenerated ? (
                                <Badge
                                    icon={<SparklesIcon className="size-3" />}
                                    label="AI"
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            ) : (
                                <Badge label="Upload" styleType="secondary-outline" weight="semibold" />
                            )}
                        </div>

                        <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                            <span>{sizeLabel}</span>

                            <div onClick={handleStopPropagation}>
                                <TagList
                                    getRequest={(_id, newTags) => ({
                                        input: {
                                            id: file.id,
                                            tags: newTags.map((tag) => ({
                                                id: tag.id == null ? undefined : String(tag.id),
                                                name: tag.name,
                                            })),
                                        },
                                    })}
                                    id={Number(file.id)}
                                    remainingTags={convertedRemainingTags}
                                    tags={convertedTags}
                                    updateTagsMutation={updateTagsMutation}
                                />
                            </div>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-4">
                    <span className="text-xs text-muted-foreground">{lastModifiedLabel}</span>

                    <div onClick={handleStopPropagation}>
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="File menu"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setSelectedFileId(Number(file.id))}
                                >
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

                                <DropdownMenuItem asChild className="dropdown-menu-item">
                                    <a download={file.name} href={file.downloadUrl} rel="noreferrer">
                                        <DownloadIcon /> Download
                                    </a>
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => onRename(file.id, file.name)}
                                >
                                    <EditIcon /> Rename
                                </DropdownMenuItem>

                                {file.publicLinkUrl ? (
                                    <>
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={handleCopyPublicLinkClick}
                                        >
                                            <LinkIcon /> Copy public link
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() => disablePublicLinkMutation.mutate({id: file.id})}
                                        >
                                            <Link2OffIcon /> Disable public link
                                        </DropdownMenuItem>
                                    </>
                                ) : (
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() => enablePublicLinkMutation.mutate({id: file.id})}
                                    >
                                        <LinkIcon /> Enable public link
                                    </DropdownMenuItem>
                                )}

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onClick={() => onDelete(file.id, file.name)}
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AssetFileListItem;
