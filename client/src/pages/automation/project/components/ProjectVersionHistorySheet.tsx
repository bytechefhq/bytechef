import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {ProjectStatus, ProjectVersion} from '@/shared/middleware/automation/configuration';
import {CheckIcon, DownloadIcon, EllipsisVerticalIcon, FileIcon, HistoryIcon, SendIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface ProjectVersionHistorySheetProps {
    deleteProjectVersionPending?: boolean;
    onDeleteProjectVersion: (projectVersion: number, onSuccess: () => void) => void;
    onRestoreProjectVersion: (projectVersion: number, onSuccess: () => void) => void;
    onSheetOpenChange: (open: boolean) => void;
    projectId: number;
    projectVersions: ProjectVersion[];
    restoreProjectVersionPending?: boolean;
    sheetOpen: boolean;
}

const formatVersionDate = (date: Date) =>
    date.toLocaleString(undefined, {day: 'numeric', hour: '2-digit', minute: '2-digit', month: 'short'});

const ProjectVersionHistorySheet = ({
    deleteProjectVersionPending,
    onDeleteProjectVersion,
    onRestoreProjectVersion,
    onSheetOpenChange,
    projectId,
    projectVersions,
    restoreProjectVersionPending,
    sheetOpen,
}: ProjectVersionHistorySheetProps) => {
    const [deleteCandidateVersion, setDeleteCandidateVersion] = useState<number | undefined>();
    const [restoreConfirmationOpen, setRestoreConfirmationOpen] = useState(false);
    const [selectedVersion, setSelectedVersion] = useState<number | undefined>();

    const handleDeleteConfirm = () => {
        if (deleteCandidateVersion === undefined) {
            return;
        }

        onDeleteProjectVersion(deleteCandidateVersion, () => {
            if (selectedVersion === deleteCandidateVersion) {
                setSelectedVersion(undefined);
            }

            setDeleteCandidateVersion(undefined);
        });
    };

    const handleRestoreConfirm = () => {
        if (selectedVersion === undefined) {
            return;
        }

        onRestoreProjectVersion(selectedVersion, () => {
            setRestoreConfirmationOpen(false);

            onSheetOpenChange(false);
        });
    };

    return (
        <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <div className="flex items-center gap-x-2">
                        <HistoryIcon className="size-5" />

                        <SheetTitle className="text-lg font-semibold">Project Version History</SheetTitle>
                    </div>

                    <SheetCloseButton />
                </header>

                <ol
                    aria-label="Project versions"
                    className="flex min-h-0 flex-1 flex-col gap-y-3 overflow-y-auto p-3"
                    role="listbox"
                >
                    {projectVersions.map((projectVersion, index) => {
                        const version = projectVersion.version!;
                        const published = projectVersion.status === ProjectStatus.Published;
                        const selected = selectedVersion === version;
                        const lastItem = index === projectVersions.length - 1;

                        return (
                            <li
                                aria-disabled={!published}
                                aria-label={`Version ${version}`}
                                aria-selected={selected}
                                className={twMerge('flex gap-x-3', published && 'cursor-pointer')}
                                key={version}
                                onClick={() => published && setSelectedVersion(version)}
                                onKeyDown={(event) => {
                                    if (published && (event.key === 'Enter' || event.key === ' ')) {
                                        event.preventDefault();

                                        setSelectedVersion(version);
                                    }
                                }}
                                role="option"
                                tabIndex={published ? 0 : -1}
                            >
                                <div className="flex w-10 shrink-0 flex-col items-center">
                                    <span
                                        className={twMerge(
                                            'mt-2 rounded-md px-2 py-0.5 text-sm font-semibold',
                                            selected && 'bg-surface-brand-primary text-white'
                                        )}
                                    >
                                        {`V${version}`}
                                    </span>

                                    {!lastItem && <div className="mt-2 w-px flex-1 bg-border" />}
                                </div>

                                <div
                                    className={twMerge(
                                        'flex flex-1 items-center gap-x-2 rounded-md border border-transparent bg-surface-neutral-primary p-3',
                                        published && 'hover:border-stroke-brand-secondary-hover',
                                        selected && 'border-stroke-brand-primary hover:border-stroke-brand-primary'
                                    )}
                                >
                                    <div className="flex min-w-0 flex-1 flex-col gap-y-2">
                                        <div className="flex items-center gap-x-2">
                                            {published ? (
                                                <Badge
                                                    icon={<SendIcon />}
                                                    label="Published"
                                                    styleType="success-outline"
                                                    weight="semibold"
                                                />
                                            ) : (
                                                <Badge
                                                    icon={<FileIcon />}
                                                    label="Draft"
                                                    styleType="warning-filled"
                                                    weight="semibold"
                                                />
                                            )}

                                            {projectVersion.publishedDate && (
                                                <span className="text-sm text-muted-foreground">
                                                    {formatVersionDate(projectVersion.publishedDate)}
                                                </span>
                                            )}
                                        </div>

                                        <p className="text-sm whitespace-pre-line">
                                            {projectVersion.description ||
                                                (published ? 'No description.' : 'Current working version.')}
                                        </p>
                                    </div>

                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button
                                                aria-label={`Version ${version} actions`}
                                                className="size-6"
                                                icon={<EllipsisVerticalIcon />}
                                                onClick={(event) => event.stopPropagation()}
                                                onKeyDown={(event) => event.stopPropagation()}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end" onClick={(event) => event.stopPropagation()}>
                                            <DropdownMenuItem
                                                icon={<DownloadIcon />}
                                                label="Export"
                                                onClick={() =>
                                                    (window.location.href = `/api/automation/internal/projects/${projectId}/export?projectVersion=${version}`)
                                                }
                                            />

                                            {published && (
                                                <>
                                                    <DropdownMenuSeparator />

                                                    <DropdownMenuItem
                                                        icon={<Trash2Icon />}
                                                        label="Delete"
                                                        onClick={() => setDeleteCandidateVersion(version)}
                                                        variant="destructive"
                                                    />
                                                </>
                                            )}
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </div>
                            </li>
                        );
                    })}
                </ol>

                <footer className="flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Cancel" onClick={() => onSheetOpenChange(false)} variant="outline" />

                    <Button
                        disabled={selectedVersion === undefined || restoreProjectVersionPending}
                        icon={restoreProjectVersionPending ? <LoadingIcon /> : <CheckIcon />}
                        label="Apply selected version"
                        onClick={() => setRestoreConfirmationOpen(true)}
                    />
                </footer>
            </SheetContent>

            {restoreConfirmationOpen && selectedVersion !== undefined && (
                <AlertDialog
                    confirmButtonVariant="default"
                    confirmLabel="Apply"
                    description={`The current draft will be replaced by the workflows of version V${selectedVersion}. Unpublished changes in the draft will be lost.`}
                    isPending={restoreProjectVersionPending}
                    onCancel={() => setRestoreConfirmationOpen(false)}
                    onConfirm={handleRestoreConfirm}
                    open
                    title={`Apply version V${selectedVersion}?`}
                />
            )}

            {deleteCandidateVersion !== undefined && (
                <AlertDialog
                    description={`This action cannot be undone. This will permanently delete version V${deleteCandidateVersion} and its workflows.`}
                    isPending={deleteProjectVersionPending}
                    onCancel={() => setDeleteCandidateVersion(undefined)}
                    onConfirm={handleDeleteConfirm}
                    open
                    title={`Delete version V${deleteCandidateVersion}?`}
                />
            )}
        </Sheet>
    );
};

export default ProjectVersionHistorySheet;
