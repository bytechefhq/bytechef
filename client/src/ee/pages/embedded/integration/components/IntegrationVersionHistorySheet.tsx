import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/ee/shared/queries/embedded/integrationVersions.queries';
import VersionHistoryTimeline from '@/shared/components/version-history/VersionHistoryTimeline';
import {CheckIcon, HistoryIcon} from 'lucide-react';
import {useState} from 'react';

import IntegrationVersionWorkflowChanges from './IntegrationVersionWorkflowChanges';

interface IntegrationVersionHistorySheetProps {
    integrationId: number;
    onClose: () => void;
    onRestoreIntegrationVersion: (integrationVersion: number, onSuccess: () => void) => void;
    restoreIntegrationVersionPending?: boolean;
}

const IntegrationVersionHistorySheet = ({
    integrationId,
    onClose,
    onRestoreIntegrationVersion,
    restoreIntegrationVersionPending,
}: IntegrationVersionHistorySheetProps) => {
    const [restoreConfirmationOpen, setRestoreConfirmationOpen] = useState(false);
    const [selectedVersion, setSelectedVersion] = useState<number | undefined>();

    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId);

    const handleRestoreConfirm = () => {
        if (selectedVersion === undefined) {
            return;
        }

        onRestoreIntegrationVersion(selectedVersion, () => {
            setRestoreConfirmationOpen(false);

            onClose();
        });
    };

    return (
        <Sheet onOpenChange={() => onClose()} open>
            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <div className="flex items-center gap-x-2">
                        <HistoryIcon className="size-5" />

                        <SheetTitle className="text-lg font-semibold">Integration Version History</SheetTitle>
                    </div>

                    <SheetCloseButton />
                </header>

                {integrationVersions && (
                    <VersionHistoryTimeline
                        ariaLabel="Integration versions"
                        items={integrationVersions.map((integrationVersion) => ({
                            description: integrationVersion.description,
                            published: integrationVersion.status === IntegrationStatus.Published,
                            publishedDate: integrationVersion.publishedDate,
                            version: integrationVersion.version!,
                        }))}
                        onSelectVersion={setSelectedVersion}
                        renderChanges={({published, version}) => (
                            <IntegrationVersionWorkflowChanges
                                draft={!published}
                                expanded
                                integrationId={integrationId}
                                integrationVersion={version}
                            />
                        )}
                        selectedVersion={selectedVersion}
                    />
                )}

                <footer className="mt-auto flex shrink-0 justify-end gap-x-2 rounded-b-md border-t border-t-border/50 bg-surface-neutral-primary p-3">
                    <Button label="Cancel" onClick={() => onClose()} variant="outline" />

                    <Button
                        disabled={selectedVersion === undefined || restoreIntegrationVersionPending}
                        icon={restoreIntegrationVersionPending ? <LoadingIcon /> : <CheckIcon />}
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
                    isPending={restoreIntegrationVersionPending}
                    onCancel={() => setRestoreConfirmationOpen(false)}
                    onConfirm={handleRestoreConfirm}
                    open
                    title={`Apply version V${selectedVersion}?`}
                />
            )}
        </Sheet>
    );
};

export default IntegrationVersionHistorySheet;
