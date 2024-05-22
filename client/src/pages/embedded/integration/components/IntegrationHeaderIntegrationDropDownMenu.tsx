import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {useCreateIntegrationWorkflowMutation} from '@/shared/mutations/embedded/integrations.mutations';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {SettingsIcon} from 'lucide-react';
import {ChangeEvent, useRef} from 'react';

const IntegrationHeaderIntegrationDropDownMenu = ({
    integrationId,
    onDelete,
    onEdit,
}: {
    integrationId: number;
    onDelete: () => void;
    onEdit: () => void;
}) => {
    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: IntegrationKeys.integration(integrationId)});

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast({
                description: 'Workflow is imported.',
            });
        },
    });

    const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            importIntegrationWorkflowMutation.mutate({
                id: integrationId,
                workflowModel: {
                    definition: await e.target.files[0].text(),
                },
            });
        }
    };

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <div>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button className="hover:bg-gray-200" size="icon" variant="ghost">
                                    <SettingsIcon className="h-5" />
                                </Button>
                            </TooltipTrigger>

                            <TooltipContent>Integration Settings</TooltipContent>
                        </Tooltip>
                    </div>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onEdit()}>Edit</DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-destructive" onClick={() => onDelete()}>
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <input className="hidden" onChange={handleFileChange} ref={hiddenFileInputRef} type="file" />
        </>
    );
};

export default IntegrationHeaderIntegrationDropDownMenu;
