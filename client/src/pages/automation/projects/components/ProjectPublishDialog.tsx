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
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project} from '@/shared/middleware/automation/configuration';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

const ProjectPublishDialog = ({onClose, project}: {onClose: () => void; project: Project}) => {
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {captureProjectPublished} = useAnalytics();

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            captureProjectPublished();

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projects,
            });

            toast('The project has been published.');

            onClose();
        },
    });

    return (
        <Dialog onOpenChange={() => onClose()} open={true}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Publish project to activate its workflows."
                        title={`Publish Project ${project.name}`}
                    />

                    <DialogBody>
                        <div className="flex flex-col space-y-2">
                            <Label>Description</Label>

                            <Textarea className="h-28" onChange={(event) => setDescription(event.target.value)} />
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            label="Publish"
                            onClick={() =>
                                publishProjectMutation.mutate({
                                    id: project.id!,
                                    publishProjectRequest: {
                                        description,
                                    },
                                })
                            }
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectPublishDialog;
