import {Button} from '@/components/ui/button';
import {DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useToast} from '@/components/ui/use-toast';
import {ProjectModel} from '@/middleware/automation/configuration';
import {usePublishProjectMutation} from '@/mutations/automation/projects.mutations';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {Dialog} from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const ProjectPublishDialog = ({onClose, project}: {onClose: () => void; project: ProjectModel}) => {
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
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

                        <DialogClose asChild>
                            <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                        </DialogClose>
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
                            disabled={!!project?.publishedDate}
                            onClick={() =>
                                publishProjectMutation.mutate({
                                    id: project.id!,
                                    publishProjectRequestModel: {
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
