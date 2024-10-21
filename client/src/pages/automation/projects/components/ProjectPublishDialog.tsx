import {Button} from '@/components/ui/button';
import {DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useToast} from '@/hooks/use-toast';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project} from '@/shared/middleware/automation/configuration';
import {usePublishProjectMutation} from '@/shared/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {Dialog} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const ProjectPublishDialog = ({onClose, project}: {onClose: () => void; project: Project}) => {
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {captureProjectPublished} = useAnalytics();

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            captureProjectPublished();

            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projects,
            });

            toast({
                description: 'The project is published.',
            });

            onClose();
        },
    });

    return (
        <Dialog onOpenChange={() => onClose()} open={true}>
            <DialogContent className="flex flex-col">
                <DialogHeader>
                    <div className="flex items-center justify-between">
                        <DialogTitle>Publish Project {project.name}</DialogTitle>
                    </div>

                    <DialogDescription>
                        Publish project to activate its workflows in one of environments.
                    </DialogDescription>
                </DialogHeader>

                <div className="flex flex-col space-y-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Description</Label>

                        <Textarea className="h-28" onChange={(event) => setDescription(event.target.value)}></Textarea>
                    </div>

                    <div className="flex justify-end">
                        <Button
                            onClick={() =>
                                publishProjectMutation.mutate({
                                    id: project.id!,
                                    publishProjectRequest: {
                                        description,
                                    },
                                })
                            }
                            size="sm"
                        >
                            Publish
                        </Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectPublishDialog;
