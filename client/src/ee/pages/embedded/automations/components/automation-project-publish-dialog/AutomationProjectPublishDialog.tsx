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
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {AutomationProjectsQuery, usePublishAutomationProjectMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

interface AutomationProjectPublishDialogProps {
    onClose: () => void;
    project: Pick<AutomationProjectType, 'id' | 'name'>;
}

const AutomationProjectPublishDialog = ({onClose, project}: AutomationProjectPublishDialogProps) => {
    const [description, setDescription] = useState<string | undefined>(undefined);

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishAutomationProjectMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjectCategories']});
            void queryClient.invalidateQueries({queryKey: ['automationProjectTags']});
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});
            void queryClient.invalidateQueries({queryKey: ['automationProjectVersions']});

            toast('The project has been published.');

            onClose();
        },
    });

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (!isOpen && !publishProjectMutation.isPending) {
                    onClose();
                }
            }}
            open={true}
        >
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Publish project to release a new version that configurations can use."
                        showCloseButton={!publishProjectMutation.isPending}
                        title={`Publish Project ${project.name}`}
                    />

                    <DialogBody>
                        <div className="flex flex-col space-y-2">
                            <Label htmlFor="automationProjectPublishDescription">Description</Label>

                            <Textarea
                                className="h-28"
                                id="automationProjectPublishDescription"
                                onChange={(event) => setDescription(event.target.value)}
                                placeholder="Describe what changed in this version, e.g. Added a Slack notification for new leads"
                            />
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton disabled={publishProjectMutation.isPending} />

                        <Button
                            disabled={publishProjectMutation.isPending}
                            icon={publishProjectMutation.isPending ? <LoadingIcon /> : undefined}
                            label="Publish"
                            onClick={() =>
                                publishProjectMutation.mutate({description: description || undefined, id: project.id})
                            }
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationProjectPublishDialog;
