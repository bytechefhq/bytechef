import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem} from '@/components/ui/command';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {cn} from '@/lib/utils';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useGetWorkflowNodeOptionsQuery} from '@/queries/platform/workflowNodeOptions.queries';
import {CaretSortIcon, CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {FocusEventHandler, ReactNode, useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import type {OptionModel, OptionsDataSourceModel} from '@/middleware/platform/configuration';

type ComboBoxItemType = {
    description?: string;
    icon?: string;
    label: string | ReactNode;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value: any;
    [key: string]: unknown;
};

interface PropertyComboBoxProps {
    description?: string;
    label?: string;
    loadDependency?: any;
    leadingIcon?: ReactNode;
    name?: string;
    onBlur?: FocusEventHandler;
    onValueChange?: (value: string) => void;
    options: Array<OptionModel>;
    optionsDataSource?: OptionsDataSourceModel;
    placeholder?: string;
    required?: boolean;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any;
}

const PropertyComboBox = ({
    description,
    label,
    leadingIcon,
    loadDependency,
    name,
    onBlur,
    onValueChange,
    options,
    optionsDataSource,
    placeholder = 'Select...',
    required,
    value,
}: PropertyComboBoxProps) => {
    const [open, setOpen] = useState(false);
    const [loadDependencyValues, setLoadDependencyValues] = useState<Array<string>>(
        Object.values(loadDependency ?? {})
    );

    const {workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {
        data: optionsData,
        isLoading,
        isRefetching,
        refetch,
    } = useGetWorkflowNodeOptionsQuery(
        {
            id: workflow.id!,
            propertyName: name!,
            workflowNodeName: currentNode.name!,
        },
        !!optionsDataSource &&
            loadDependencyValues.reduce((enabled: boolean, loadDependencyValue: string) => {
                return loadDependencyValue !== undefined;
            }, true)
    );

    if (optionsData) {
        options = optionsData.map((option) => ({
            description: option.description,
            label: option.label ?? option.value,
            value: option.value.toString(),
        }));
    }

    const items = options as Array<ComboBoxItemType>;

    const item = items?.find((item) => item.value === value);

    placeholder = loadDependencyValues?.length ? `Depends on ${Object.keys(loadDependency)}` : placeholder;

    useEffect(() => {
        if (loadDependency && typeof loadDependency === 'object') {
            setLoadDependencyValues(Object.values(loadDependency));
        }
    }, [loadDependency]);

    useEffect(() => {
        if (loadDependencyValues?.length) {
            refetch();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [loadDependencyValues]);

    return (
        <fieldset className="w-full space-y-2">
            {label && (
                <div className="flex items-center">
                    <Label className={twMerge(description && 'mr-1')} htmlFor={name}>
                        {label}

                        {required && <span className="leading-3 text-red-500">*</span>}
                    </Label>

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <QuestionMarkCircledIcon />
                            </TooltipTrigger>

                            <TooltipContent>{description}</TooltipContent>
                        </Tooltip>
                    )}
                </div>
            )}

            <Popover onOpenChange={setOpen} open={open}>
                <PopoverTrigger asChild onBlur={onBlur}>
                    <Button
                        aria-expanded={open}
                        className={twMerge('relative w-full justify-between', leadingIcon && 'relative')}
                        disabled={isRefetching}
                        name={name}
                        role="combobox"
                        variant="outline"
                    >
                        {leadingIcon && (
                            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r border-gray-300 bg-gray-100 px-3">
                                {leadingIcon}
                            </div>
                        )}

                        {optionsDataSource && loadDependency && isRefetching && !item?.label && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Refetching...
                            </span>
                        )}

                        {optionsDataSource && isLoading && (
                            <span className={twMerge('flex items-center', leadingIcon && 'ml-9')}>
                                <LoadingIcon /> Loading...
                            </span>
                        )}

                        {!optionsDataSource && !options.length && (
                            <span className="rounded-md border p-2 text-sm text-muted-foreground">
                                No options available
                            </span>
                        )}

                        {((optionsDataSource && !isLoading) || !optionsDataSource) && (
                            <>
                                {value ? (
                                    <span className={twMerge('flex w-full items-center', leadingIcon && 'ml-9')}>
                                        {item?.icon && <InlineSVG className="mr-2 size-6 flex-none" src={item?.icon} />}

                                        {item?.label}
                                    </span>
                                ) : (
                                    <span className={twMerge(leadingIcon && 'ml-9')}>{placeholder}</span>
                                )}
                            </>
                        )}

                        <CaretSortIcon className="ml-2 size-4 shrink-0 opacity-50" />
                    </Button>
                </PopoverTrigger>

                <PopoverContent align="start" className="min-w-combo-box-popper-anchor-width p-0" side="bottom">
                    <Command>
                        <CommandInput className="h-9 border-none ring-0" placeholder="Search..." />

                        <CommandEmpty>No item found.</CommandEmpty>

                        <CommandGroup>
                            <ScrollArea className="h-72 w-full">
                                {items?.map((comboBoxItem) => (
                                    <CommandItem
                                        key={comboBoxItem.value}
                                        onSelect={() => {
                                            setOpen(false);

                                            if (onValueChange) {
                                                onValueChange(comboBoxItem.value);
                                            }
                                        }}
                                        value={comboBoxItem.value}
                                    >
                                        {comboBoxItem.icon && (
                                            <InlineSVG className="mr-2 size-6 flex-none" src={comboBoxItem.icon} />
                                        )}

                                        {comboBoxItem.description ? (
                                            <div className="flex flex-col gap-1">
                                                <div>{comboBoxItem.label}</div>

                                                <div className="text-xs text-muted-foreground">
                                                    {comboBoxItem.description}
                                                </div>
                                            </div>
                                        ) : (
                                            <span>{comboBoxItem.label}</span>
                                        )}

                                        <CheckIcon
                                            className={cn(
                                                'ml-auto size-4',
                                                comboBoxItem.value === value ? 'opacity-100' : 'opacity-0'
                                            )}
                                        />
                                    </CommandItem>
                                ))}
                            </ScrollArea>
                        </CommandGroup>
                    </Command>
                </PopoverContent>
            </Popover>
        </fieldset>
    );
};

export default PropertyComboBox;
