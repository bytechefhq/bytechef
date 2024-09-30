import {Button} from '@/components/ui/button';
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useToast} from '@/components/ui/use-toast';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Integration} from '@/shared/middleware/embedded/configuration';
import {usePublishIntegrationMutation} from '@/shared/mutations/embedded/integrations.mutations';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
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
                description: 'The integration is published.',
            });

            onClose();
        },
    });

    return (
        <Dialog onOpenChange={() => onClose()} open={true}>
            <DialogContent className="flex flex-col">
                <DialogHeader>
                    <div className="flex items-center justify-between">
                        <DialogTitle>Publish Integration {integration.componentName}</DialogTitle>
                    </div>

                    <DialogDescription>
                        Publish integration to activate its workflows in one of environments.
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
                                publishIntegrationMutation.mutate({
                                    id: integration.id!,
                                    publishIntegrationRequest: {
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

export default IntegrationPublishDialog;
