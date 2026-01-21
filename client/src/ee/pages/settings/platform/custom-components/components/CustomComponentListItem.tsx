import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
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
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface CustomComponentItemProps {
    customComponent: CustomComponent;
}

const Title = ({customComponent}: {customComponent: CustomComponent}) => {
    return (
        <div className="flex flex-col items-start space-y-1">
            <div className="flex space-x-1">
                <div className="font-semibold">{customComponent.title}</div>

                {customComponent.componentVersion && (
                    <Badge
                        className="flex items-center justify-center"
                        label={`v${customComponent.componentVersion}`}
                        styleType="secondary-filled"
                        weight="semibold"
                    />
                )}
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
                        {customComponent.language && (
                            <Badge
                                className="flex w-24 items-center justify-center"
                                label={customComponent.language}
                                styleType="secondary-filled"
                                weight="semibold"
                            />
                        )}
                    </div>

                    <div className="flex flex-col items-end gap-y-4">
                        <Switch checked={customComponent.enabled} onCheckedChange={handleOnCheckedChange} />

                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {customComponent.lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${customComponent.lastModifiedDate?.toLocaleDateString()} ${customComponent.lastModifiedDate?.toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Created Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                                size="icon"
                                variant="ghost"
                            />
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
