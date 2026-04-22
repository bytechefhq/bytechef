import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceFilesStore} from '@/pages/automation/workspace-files/stores/useWorkspaceFilesStore';
import {WorkspaceFile, WorkspaceFileSource} from '@/shared/middleware/graphql';
import {
    DownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    FileIcon,
    FileImageIcon,
    FileTextIcon,
    SparklesIcon,
    Trash2Icon,
} from 'lucide-react';
import {MouseEvent} from 'react';

interface WorkspaceFileRowProps {
    file: WorkspaceFile;
    onDelete: (fileId: string, fileName: string) => void;
    onRename: (fileId: string, fileName: string) => void;
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

const WorkspaceFileRow = ({file, onDelete, onRename}: WorkspaceFileRowProps) => {
    const setSelectedFileId = useWorkspaceFilesStore((state) => state.setSelectedFileId);

    const sizeLabel = formatBytes(Number(file.sizeBytes));
    const lastModifiedLabel = formatDate(file.lastModifiedDate);

    const handleRowClick = () => {
        setSelectedFileId(Number(file.id));
    };

    const handleStopPropagation = (event: MouseEvent) => {
        event.stopPropagation();
    };

    return (
        <div
            className="group flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-destructive-foreground"
            data-testid={`workspace-file-row-${file.id}`}
            onClick={handleRowClick}
        >
            <div className="flex flex-1 items-center py-4">
                <div className="flex flex-1 items-center gap-3">
                    {pickMimeIcon(file.mimeType)}

                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            <span className="text-base font-semibold">{file.name}</span>

                            {file.source === WorkspaceFileSource.AiGenerated ? (
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

                            {file.tags.length > 0 && (
                                <span className="flex items-center gap-1">
                                    {file.tags.map((tag) => (
                                        <Badge
                                            key={tag.id}
                                            label={tag.name}
                                            styleType="secondary-outline"
                                            weight="regular"
                                        />
                                    ))}
                                </span>
                            )}
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

export default WorkspaceFileRow;
