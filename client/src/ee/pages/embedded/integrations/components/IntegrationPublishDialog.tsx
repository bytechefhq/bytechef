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
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {usePublishIntegrationMutation} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

const IntegrationPublishDialog = ({integration, onClose}: {integration: Integration; onClose: () => void}) => {
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {captureIntegrationPublished} = useAnalytics();

    const queryClient = useQueryClient();

    const publishIntegrationMutation = usePublishIntegrationMutation({
        onSuccess: () => {
            captureIntegrationPublished();

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });

            toast('The integration has been published.');

            onClose();
        },
    });

    return (
        <Dialog onOpenChange={() => onClose()} open={true}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Publish integration to activate its workflows."
                        title={`Publish Integration ${integration.componentName}`}
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
                                publishIntegrationMutation.mutate({
                                    id: integration.id!,
                                    publishIntegrationRequest: {
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

export default IntegrationPublishDialog;
