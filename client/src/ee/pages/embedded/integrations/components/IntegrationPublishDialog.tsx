import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {usePublishIntegrationMutation} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useToast} from '@/hooks/use-toast';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const IntegrationPublishDialog = ({integration, onClose}: {integration: Integration; onClose: () => void}) => {
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {captureIntegrationPublished} = useAnalytics();

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishIntegrationMutation = usePublishIntegrationMutation({
        onSuccess: () => {
            captureIntegrationPublished();

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });

            toast({
                description: 'The integration has been published.',
            });

            onClose();
        },
    });

    return (
        <Dialog onOpenChange={() => onClose()} open={true}>
            <DialogContent className="flex flex-col">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Publish Integration {integration.componentName}</DialogTitle>

                        <DialogDescription>Publish integration to activate its workflows.</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="flex flex-col space-y-4">
                    <div className="flex flex-col space-y-2">
                        <Label>Description</Label>

                        <Textarea className="h-28" onChange={(event) => setDescription(event.target.value)}></Textarea>
                    </div>

                    <div className="flex justify-end">
                        <Button
                            label="Publish"
                            onClick={() =>
                                publishIntegrationMutation.mutate({
                                    id: integration.id!,
                                    publishIntegrationRequest: {
                                        description,
                                    },
                                })
                            }
                        />
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationPublishDialog;
