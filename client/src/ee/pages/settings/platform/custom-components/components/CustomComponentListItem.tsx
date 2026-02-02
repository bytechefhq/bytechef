import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import CustomComponentDeleteAlertDialog from '@/ee/pages/settings/platform/custom-components/components/CustomComponentDeleteAlertDialog';
import {
    CustomComponent,
    useCustomComponentDefinitionQuery,
    useDeleteCustomComponentMutation,
    useEnableCustomComponentMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, ChevronRightIcon, EllipsisVerticalIcon, ZapIcon} from 'lucide-react';
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
    const [isExpanded, setIsExpanded] = useState(false);

    const queryClient = useQueryClient();

    const deleteCustomComponentMutation = useDeleteCustomComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['customComponents'],
            });
        },
    });

    const enableCustomComponentMutation = useEnableCustomComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['customComponents'],
            });
        },
    });

    const {data: definitionData, isLoading: isLoadingDefinition} = useCustomComponentDefinitionQuery(
        {id: customComponent.id},
        {
            enabled: isExpanded,
        }
    );

    const handleAlertDeleteDialogClick = () => {
        if (customComponent.id) {
            deleteCustomComponentMutation.mutate({id: customComponent.id});

            setShowDeleteDialog(false);
        }
    };

    const handleOnCheckedChange = (value: boolean) => {
        enableCustomComponentMutation.mutate({
            enable: value,
            id: customComponent.id,
        });
    };

    const actions = definitionData?.customComponentDefinition?.actions ?? [];
    const triggers = definitionData?.customComponentDefinition?.triggers ?? [];

    return (
        <Collapsible onOpenChange={setIsExpanded} open={isExpanded}>
            <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
                <div className="flex items-center justify-between">
                    <div className="flex flex-1 items-center gap-x-2">
                        <CollapsibleTrigger asChild>
                            <Button
                                className="size-6 p-0"
                                icon={
                                    isExpanded ? (
                                        <ChevronDownIcon className="size-4 text-gray-500" />
                                    ) : (
                                        <ChevronRightIcon className="size-4 text-gray-500" />
                                    )
                                }
                                size="icon"
                                variant="ghost"
                            />
                        </CollapsibleTrigger>

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
                            <Switch
                                checked={customComponent.enabled ?? false}
                                onCheckedChange={handleOnCheckedChange}
                            />

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {customComponent.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${new Date(customComponent.lastModifiedDate).toLocaleDateString()} ${new Date(customComponent.lastModifiedDate).toLocaleTimeString()}`}
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

                <CollapsibleContent>
                    <div className="ml-8 mt-4 border-t pt-4">
                        {isLoadingDefinition ? (
                            <div className="flex items-center gap-2 text-sm text-gray-500">
                                <LoadingIcon className="size-4" />
                                Loading component definition...
                            </div>
                        ) : (
                            <div className="grid grid-cols-2 gap-6">
                                <div>
                                    <h4 className="mb-2 flex items-center gap-1 text-sm font-semibold text-gray-700">
                                        <ZapIcon className="size-4" />

                                        <span>Actions ({actions.length})</span>
                                    </h4>

                                    {actions.length > 0 ? (
                                        <ul className="space-y-2">
                                            {actions.map((action) => (
                                                <li className="rounded bg-gray-50 p-2" key={action.name}>
                                                    <div className="text-sm font-medium">
                                                        {action.title || action.name}
                                                    </div>

                                                    <div className="text-xs text-gray-500">{action.name}</div>

                                                    {action.description && (
                                                        <div className="mt-1 text-xs text-gray-600">
                                                            {action.description}
                                                        </div>
                                                    )}
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="text-sm text-gray-500">No actions defined</p>
                                    )}
                                </div>

                                <div>
                                    <h4 className="mb-2 flex items-center gap-1 text-sm font-semibold text-gray-700">
                                        <ZapIcon className="size-4" />

                                        <span>Triggers ({triggers.length})</span>
                                    </h4>

                                    {triggers.length > 0 ? (
                                        <ul className="space-y-2">
                                            {triggers.map((trigger) => (
                                                <li className="rounded bg-gray-50 p-2" key={trigger.name}>
                                                    <div className="text-sm font-medium">
                                                        {trigger.title || trigger.name}
                                                    </div>

                                                    <div className="text-xs text-gray-500">{trigger.name}</div>

                                                    {trigger.description && (
                                                        <div className="mt-1 text-xs text-gray-600">
                                                            {trigger.description}
                                                        </div>
                                                    )}
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="text-sm text-gray-500">No triggers defined</p>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                </CollapsibleContent>

                {showDeleteDialog && (
                    <CustomComponentDeleteAlertDialog
                        onClose={() => setShowDeleteDialog(false)}
                        onDelete={handleAlertDeleteDialogClick}
                    />
                )}
            </div>
        </Collapsible>
    );
};

export default CustomComponentListItem;
