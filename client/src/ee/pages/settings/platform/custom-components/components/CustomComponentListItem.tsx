import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    CustomComponent,
    CustomComponentLanguage,
    CustomComponentStatus,
    useCustomComponentDefinitionQuery,
    useDeleteCustomComponentMutation,
    useEnableCustomComponentMutation,
} from '@/shared/middleware/graphql';
import getCodeWorkflowLanguageLabel from '@/shared/util/codeWorkflowLanguage-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, ChevronRightIcon, EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import CustomComponentDefinitionItemList from './CustomComponentDefinitionItemList';

interface CustomComponentItemProps {
    customComponent: CustomComponent;
}

const Title = ({customComponent, opensDetail}: {customComponent: CustomComponent; opensDetail: boolean}) => {
    return (
        <div className="flex flex-col items-start space-y-1">
            <div
                className={twMerge(
                    'font-semibold',
                    opensDetail && 'group-hover:underline group-has-[[data-expand-trigger]:hover]:no-underline'
                )}
            >
                {customComponent.title}
            </div>

            <div className="text-sm">{customComponent.name}</div>
        </div>
    );
};

const CustomComponentListItem = ({customComponent}: CustomComponentItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [isExpanded, setIsExpanded] = useState(false);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const isJavaComponent = customComponent.language === CustomComponentLanguage.Java;

    const deleteCustomComponentMutation = useDeleteCustomComponentMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ['customComponents'],
            });
        },
    });

    const enableCustomComponentMutation = useEnableCustomComponentMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ['customComponents'],
            });
        },
    });

    const {
        data: definitionData,
        isError: isDefinitionError,
        isLoading: isLoadingDefinition,
    } = useCustomComponentDefinitionQuery(
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

    const handleRowClick = () => {
        if (!isJavaComponent) {
            void navigate(String(customComponent.id));
        }
    };

    const handleRowKeyDown = (event: React.KeyboardEvent) => {
        if ((event.key === 'Enter' || event.key === ' ') && !isJavaComponent) {
            event.preventDefault();
            handleRowClick();
        }
    };

    const actions = definitionData?.customComponentDefinition?.actions ?? [];
    const triggers = definitionData?.customComponentDefinition?.triggers ?? [];

    return (
        <Collapsible
            className="mb-2 w-full rounded border border-border/50"
            onOpenChange={setIsExpanded}
            open={isExpanded}
        >
            <div className="group w-full rounded-md px-3 py-4 hover:bg-surface-neutral-primary-hover">
                <div className="flex items-center justify-between">
                    <div className="flex flex-1 items-center gap-x-2">
                        <CollapsibleTrigger asChild>
                            <Button
                                aria-label={isExpanded ? 'Hide actions and triggers' : 'Show actions and triggers'}
                                className="size-6 p-0"
                                data-expand-trigger
                                icon={
                                    isExpanded ? (
                                        <ChevronDownIcon className="size-4 text-content-neutral-secondary" />
                                    ) : (
                                        <ChevronRightIcon className="size-4 text-content-neutral-secondary" />
                                    )
                                }
                                size="icon"
                                variant="ghost"
                            />
                        </CollapsibleTrigger>

                        <div
                            className={twMerge(
                                'flex-1',
                                !isJavaComponent &&
                                    'cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 focus-visible:outline-none'
                            )}
                            onClick={isJavaComponent ? undefined : handleRowClick}
                            onKeyDown={handleRowKeyDown}
                            role={!isJavaComponent ? 'button' : undefined}
                            tabIndex={!isJavaComponent ? 0 : undefined}
                        >
                            <div className="flex min-h-8 items-center justify-between">
                                <div className="flex w-full items-center justify-between">
                                    {customComponent.description ? (
                                        <Tooltip>
                                            <TooltipTrigger>
                                                <Title
                                                    customComponent={customComponent}
                                                    opensDetail={!isJavaComponent}
                                                />
                                            </TooltipTrigger>

                                            <TooltipContent>{customComponent.description}</TooltipContent>
                                        </Tooltip>
                                    ) : (
                                        <div className="space-x-1">
                                            <Title customComponent={customComponent} opensDetail={!isJavaComponent} />
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex shrink-0 items-center justify-end gap-x-6 [&>:last-child]:-ml-3">
                        <div className="flex w-28 justify-end">
                            {customComponent.language && (
                                <Badge
                                    className="flex items-center justify-center uppercase"
                                    label={
                                        getCodeWorkflowLanguageLabel(customComponent.language) ??
                                        customComponent.language
                                    }
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            )}
                        </div>

                        <div className="flex w-32 justify-end">
                            {customComponent.status && (
                                <Badge
                                    className={twMerge(
                                        'flex space-x-1',
                                        customComponent.status !== CustomComponentStatus.Draft && 'border-transparent'
                                    )}
                                    styleType={
                                        customComponent.status === CustomComponentStatus.Draft
                                            ? 'secondary-filled'
                                            : 'success-outline'
                                    }
                                    weight="semibold"
                                >
                                    {customComponent.componentVersion && (
                                        <span>V{customComponent.componentVersion}</span>
                                    )}

                                    <span>
                                        {customComponent.status === CustomComponentStatus.Draft ? 'DRAFT' : 'PUBLISHED'}
                                    </span>
                                </Badge>
                            )}
                        </div>

                        <div className="flex w-56 shrink-0 flex-col items-end gap-y-2">
                            <div className="flex min-h-8 items-center">
                                <Switch
                                    checked={customComponent.enabled ?? false}
                                    onCheckedChange={handleOnCheckedChange}
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
                                    {customComponent.lastModifiedDate ? (
                                        <span className="text-xs whitespace-nowrap">
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
                                <DropdownMenuItem onClick={() => setShowDeleteDialog(true)} variant="destructive">
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>

                <CollapsibleContent>
                    <div className="mt-4 ml-8">
                        {isLoadingDefinition && (
                            <div className="flex items-center gap-2 text-sm text-content-neutral-secondary">
                                <LoadingIcon className="size-4" />
                                Loading component definition...
                            </div>
                        )}

                        {isDefinitionError && (
                            <p className="text-sm text-content-neutral-secondary">
                                {isJavaComponent
                                    ? 'The component definition could not be loaded.'
                                    : 'The component definition could not be loaded. Open the component to fix its source.'}
                            </p>
                        )}

                        {!isLoadingDefinition && !isDefinitionError && (
                            <Tabs defaultValue="actions">
                                <TabsList>
                                    <TabsTrigger value="actions">Actions ({actions.length})</TabsTrigger>

                                    <TabsTrigger value="triggers">Triggers ({triggers.length})</TabsTrigger>
                                </TabsList>

                                <TabsContent className="pt-2" value="actions">
                                    <CustomComponentDefinitionItemList
                                        emptyMessage="No actions defined"
                                        items={actions}
                                    />
                                </TabsContent>

                                <TabsContent className="pt-2" value="triggers">
                                    <CustomComponentDefinitionItemList
                                        emptyMessage="No triggers defined"
                                        items={triggers}
                                    />
                                </TabsContent>
                            </Tabs>
                        )}
                    </div>
                </CollapsibleContent>

                {showDeleteDialog && (
                    <AlertDialog
                        description="This action cannot be undone. This will permanently delete the custom component."
                        onCancel={() => setShowDeleteDialog(false)}
                        onConfirm={handleAlertDeleteDialogClick}
                        open
                    />
                )}
            </div>
        </Collapsible>
    );
};

export default CustomComponentListItem;
