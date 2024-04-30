import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {IntegrationModel} from '@/middleware/embedded/configuration';
import {usePublishIntegrationMutation} from '@/mutations/embedded/integrations.mutations';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CircleDotIcon} from 'lucide-react';
import {useState} from 'react';

const IntegrationPublishPopover = ({integration}: {integration: IntegrationModel}) => {
    const [open, setOpen] = useState(false);
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishIntegrationMutation = usePublishIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(integration.id!),
            });

            toast({
                description: 'The integration is published.',
            });

            setOpen(false);
        },
    });

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button
                    className="hover:bg-gray-200"
                    disabled={!!integration?.publishedDate}
                    size="icon"
                    variant="ghost"
                >
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <CircleDotIcon className="h-5" />
                        </TooltipTrigger>

                        <TooltipContent>
                            {integration?.publishedDate ? `The integration is published` : `Publish the integration`}
                        </TooltipContent>
                    </Tooltip>
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="flex h-full w-96 flex-col justify-between space-y-4">
                <h3 className="font-semibold">Publish Integration</h3>

                <div className="flex-1">
                    <Label>Description</Label>

                    <Textarea className="h-28" onChange={(event) => setDescription(event.target.value)}></Textarea>
                </div>

                <div className="flex justify-end">
                    <Button
                        disabled={!!integration?.publishedDate}
                        onClick={() =>
                            publishIntegrationMutation.mutate({
                                id: integration.id!,
                                publishIntegrationRequestModel: {
                                    description,
                                },
                            })
                        }
                        size="sm"
                    >
                        Publish
                    </Button>
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default IntegrationPublishPopover;
