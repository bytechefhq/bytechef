import AlertDialog from '@/components/AlertDialog';
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
import VersionHistoryTimeline from '@/shared/components/version-history/VersionHistoryTimeline';
import {ProjectStatus, ProjectVersion} from '@/shared/middleware/automation/configuration';
import {CheckIcon, DownloadIcon, EllipsisVerticalIcon, HistoryIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import ProjectVersionWorkflowChanges from './ProjectVersionWorkflowChanges';

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

                <VersionHistoryTimeline
                    ariaLabel="Project versions"
                    items={projectVersions.map((projectVersion) => ({
                        description: projectVersion.description,
                        published: projectVersion.status === ProjectStatus.Published,
                        publishedDate: projectVersion.publishedDate,
                        version: projectVersion.version!,
                    }))}
                    onSelectVersion={setSelectedVersion}
                    renderActions={({published, version}) => (
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
                    )}
                    renderChanges={({published, version}) => (
                        <ProjectVersionWorkflowChanges
                            draft={!published}
                            expanded
                            projectId={projectId}
                            projectVersion={version}
                        />
                    )}
                    selectedVersion={selectedVersion}
                />

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
