import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import VersionHistoryTimeline from '@/shared/components/version-history/VersionHistoryTimeline';
import {useAutomationProjectVersionsQuery} from '@/shared/middleware/graphql';
import {CheckIcon, HistoryIcon} from 'lucide-react';
import {useState} from 'react';

import AutomationProjectVersionWorkflowChanges from './AutomationProjectVersionWorkflowChanges';

interface AutomationProjectVersionHistorySheetPropsI {
    onClose: () => void;
    onRestoreProjectVersion: (projectVersion: number, onSuccess: () => void) => void;
    open: boolean;
    projectId: string;
    restoreProjectVersionPending?: boolean;
}

const AutomationProjectVersionHistorySheet = ({
    onClose,
    onRestoreProjectVersion,
    open,
    projectId,
    restoreProjectVersionPending,
}: AutomationProjectVersionHistorySheetPropsI) => {
    const [restoreConfirmationOpen, setRestoreConfirmationOpen] = useState(false);
    const [selectedVersion, setSelectedVersion] = useState<number | undefined>();

    const {data: versions} = useAutomationProjectVersionsQuery({id: projectId}, {enabled: open});

    const projectVersions = versions?.automationProjectVersions;

    const handleClose = () => {
        setRestoreConfirmationOpen(false);
        setSelectedVersion(undefined);

        onClose();
    };

    const handleRestoreConfirm = () => {
        if (selectedVersion === undefined) {
            return;
        }

        onRestoreProjectVersion(selectedVersion, handleClose);
    };

    return (
        <Sheet onOpenChange={(value) => !value && handleClose()} open={open}>
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

                {projectVersions && (
                    <VersionHistoryTimeline
                        ariaLabel="Project versions"
                        items={projectVersions.map((projectVersion) => ({
                            description: projectVersion.description,
                            published: projectVersion.status === 'PUBLISHED',
                            publishedDate: projectVersion.publishedDate
                                ? new Date(projectVersion.publishedDate)
                                : undefined,
                            version: projectVersion.version,
                        }))}
                        onSelectVersion={setSelectedVersion}
                        renderChanges={({published, version}) => (
                            <AutomationProjectVersionWorkflowChanges
                                draft={!published}
                                expanded
                                projectId={projectId}
                                projectVersion={version}
                            />
                        )}
                        selectedVersion={selectedVersion}
                    />
                )}

                <footer className="mt-auto flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Cancel" onClick={handleClose} variant="outline" />

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
        </Sheet>
    );
};

export default AutomationProjectVersionHistorySheet;
