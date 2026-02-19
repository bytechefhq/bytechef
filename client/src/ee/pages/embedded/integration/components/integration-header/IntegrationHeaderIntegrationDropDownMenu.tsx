import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useCreateIntegrationWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useToast} from '@/hooks/use-toast';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
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

    const {captureIntegrationWorkflowImported} = useAnalytics();

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            captureIntegrationWorkflowImported();

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
                workflow: {
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
                                <Button className="hover:bg-background/70 [&_svg]:size-5" size="icon" variant="ghost">
                                    <SettingsIcon />
                                </Button>
                            </TooltipTrigger>

                            <TooltipContent>Integration Settings</TooltipContent>
                        </Tooltip>
                    </div>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onEdit()}>Edit</DropdownMenuItem>

                    <DropdownMenuItem
                        onClick={() => {
                            if (hiddenFileInputRef.current) {
                                hiddenFileInputRef.current.click();
                            }
                        }}
                    >
                        Import Workflow
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-destructive" onClick={() => onDelete()}>
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <input
                accept=".json,.yaml,.yml"
                className="hidden"
                onChange={handleFileChange}
                ref={hiddenFileInputRef}
                type="file"
            />
        </>
    );
};

export default IntegrationHeaderIntegrationDropDownMenu;
