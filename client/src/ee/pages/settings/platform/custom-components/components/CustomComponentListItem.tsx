import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import CustomComponentDeleteAlertDialog from '@/ee/pages/settings/platform/custom-components/components/CustomComponentDeleteAlertDialog';
import {
    useDeleteCustomComponentMutation,
    useEnableCustomComponentMutation,
} from '@/ee/pages/settings/platform/custom-components/mutations/customComponents.mutations';
import {CustomComponentKeys} from '@/ee/pages/settings/platform/custom-components/queries/customComponents.queries';
import {CustomComponent} from '@/ee/shared/middleware/platform/custom-component';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';

interface CustomComponentItemProps {
    customComponent: CustomComponent;
}

const Title = ({customComponent}: {customComponent: CustomComponent}) => {
    return (
        <div className="flex flex-col items-start space-y-1">
            <div className="flex space-x-1">
                <div className="font-semibold">{customComponent.title}</div>

                <Badge className="flex items-center justify-center" variant="secondary">
                    {customComponent.componentVersion}
                </Badge>
            </div>

            <div className="text-sm">{customComponent.name}</div>
        </div>
    );
};

const CustomComponentListItem = ({customComponent}: CustomComponentItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteCustomComponentMutation = useDeleteCustomComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: CustomComponentKeys.customComponents,
            });
        },
    });

    const enableCustomComponentMutation = useEnableCustomComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: CustomComponentKeys.customComponents,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (customComponent.id) {
            deleteCustomComponentMutation.mutate(customComponent.id);

            setShowDeleteDialog(false);
        }
    };

    const handleOnCheckedChange = (value: boolean) => {
        enableCustomComponentMutation.mutate({
            enable: value,
            id: customComponent.id!,
        });
    };

    return (
        <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
            <div className="flex items-center justify-between">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center justify-between">
                            {customComponent.description ? (
                                <Tooltip>
                                    <TooltipTrigger>
                                        <Title customComponent={customComponent} />
                                    </TooltipTrigger>

                                    <TooltipContent>{customComponent.description}</TooltipContent>
                                </Tooltip>
                            ) : (
                                <div className="space-x-1">
                                    <Title customComponent={customComponent} />
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    <div>
                        <Badge className="flex w-24 items-center justify-center" variant="secondary">
                            {customComponent.language}
                        </Badge>
                    </div>

                    <div className="flex flex-col items-end gap-y-4">
                        <Switch checked={customComponent.enabled} onCheckedChange={handleOnCheckedChange} />

                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {customComponent.lastModifiedDate ? (
                                    <>
                                        <CalendarIcon
                                            aria-hidden="true"
                                            className="mr-0.5 size-3.5 shrink-0 text-gray-400"
                                        />

                                        <span className="text-xs">
                                            {`Updated at ${customComponent.lastModifiedDate?.toLocaleDateString()} ${customComponent.lastModifiedDate?.toLocaleTimeString()}`}
                                        </span>
                                    </>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Created Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            {showDeleteDialog && (
                <CustomComponentDeleteAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={handleAlertDeleteDialogClick}
                />
            )}
        </div>
    );
};

export default CustomComponentListItem;
