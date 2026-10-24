import Badge from '@/components/Badge/Badge';
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
import LoadingIcon from '@/components/LoadingIcon';
import {clearWorkflowHistory} from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useRestoreProjectVersionWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    ProjectVersionKeys,
    useGetProjectWorkflowVersionsQuery,
} from '@/shared/queries/automation/projectVersions.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {HistoryIcon, SendIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

interface LoadWorkflowVersionDialogProps {
    onClose: () => void;
    projectId: number;
    workflowUuid: string;
}

const formatVersionDate = (date: Date) =>
    date.toLocaleString(undefined, {day: 'numeric', hour: '2-digit', minute: '2-digit', month: 'short'});

const LoadWorkflowVersionDialog = ({onClose, projectId, workflowUuid}: LoadWorkflowVersionDialogProps) => {
    const [selectedVersion, setSelectedVersion] = useState<number | undefined>();

    const queryClient = useQueryClient();

    const {data: projectVersions, isLoading} = useGetProjectWorkflowVersionsQuery(projectId, workflowUuid);

    const restoreProjectVersionWorkflowMutation = useRestoreProjectVersionWorkflowMutation({
        onSuccess: (_, {projectVersion}) => {
            clearWorkflowHistory();

            useWorkflowNodeDetailsPanelStore.getState().reset();

            void queryClient.invalidateQueries({queryKey: ProjectKeys.project(projectId)});
            void queryClient.invalidateQueries({queryKey: ProjectVersionKeys.projectProjectVersions(projectId)});

            toast(`Workflow loaded from version V${projectVersion} into the draft.`);

            onClose();
        },
    });

    const handleLoadClick = () => {
        if (selectedVersion === undefined) {
            return;
        }

        restoreProjectVersionWorkflowMutation.mutate({id: projectId, projectVersion: selectedVersion, workflowUuid});
    };

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open && !restoreProjectVersionWorkflowMutation.isPending) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Replace the draft of this workflow with its content from a published version. Other workflows in the project are not changed."
                        icon={<HistoryIcon />}
                        title="Load Previous Version"
                    />

                    <DialogBody>
                        {isLoading && <LoadingIcon />}

                        {!isLoading && !projectVersions?.length && (
                            <p className="text-sm text-muted-foreground">
                                This workflow is not part of any published version yet.
                            </p>
                        )}

                        {!!projectVersions?.length && (
                            <ul aria-label="Published versions" className="flex flex-col gap-y-2" role="listbox">
                                {projectVersions.map((projectVersion) => {
                                    const version = projectVersion.version!;
                                    const selected = selectedVersion === version;

                                    return (
                                        <li
                                            aria-label={`Version ${version}`}
                                            aria-selected={selected}
                                            className={twMerge(
                                                'flex cursor-pointer flex-col gap-y-1 rounded-md border p-3 hover:border-stroke-brand-secondary-hover',
                                                selected &&
                                                    'border-stroke-brand-primary hover:border-stroke-brand-primary'
                                            )}
                                            key={version}
                                            onClick={() => setSelectedVersion(version)}
                                            onKeyDown={(event) => {
                                                if (event.key === 'Enter' || event.key === ' ') {
                                                    event.preventDefault();

                                                    setSelectedVersion(version);
                                                }
                                            }}
                                            role="option"
                                            tabIndex={0}
                                        >
                                            <div className="flex items-center gap-x-2">
                                                <span className="text-sm font-semibold">{`V${version}`}</span>

                                                <Badge
                                                    className="border-transparent"
                                                    icon={<SendIcon />}
                                                    label="Published"
                                                    styleType="success-outline"
                                                    weight="semibold"
                                                />

                                                {projectVersion.publishedDate && (
                                                    <span className="text-sm text-muted-foreground">
                                                        {formatVersionDate(projectVersion.publishedDate)}
                                                    </span>
                                                )}
                                            </div>

                                            <p className="text-sm whitespace-pre-line">
                                                {projectVersion.description || 'No description.'}
                                            </p>
                                        </li>
                                    );
                                })}
                            </ul>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton disabled={restoreProjectVersionWorkflowMutation.isPending} />

                        <Button
                            disabled={selectedVersion === undefined || restoreProjectVersionWorkflowMutation.isPending}
                            icon={restoreProjectVersionWorkflowMutation.isPending ? <LoadingIcon /> : undefined}
                            label="Load into draft"
                            onClick={handleLoadClick}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default LoadWorkflowVersionDialog;
