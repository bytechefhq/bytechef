import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {FormControl, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasicModel, ComponentDefinitionModel} from '@/shared/middleware/platform/configuration';
import {ControllerRenderProps} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import {ConnectionDialogFormProps} from './ConnectionDialog';

const ComponentSelectionInput = ({
    componentDefinition,
    field,
    handleComponentDefinitionChange,
    items,
    selectedComponentDefinition,
}: {
    componentDefinition?: ComponentDefinitionModel;
    field?: ControllerRenderProps<ConnectionDialogFormProps, 'componentName'>;
    handleComponentDefinitionChange?: (componentDefinition: ComponentDefinitionBasicModel) => void;
    items?: Array<ComboBoxItemType>;
    selectedComponentDefinition?: ComponentDefinitionBasicModel;
}) => (
    <FormItem>
        <FormLabel>Component</FormLabel>

        <FormControl>
            {items && items?.length > 1 && handleComponentDefinitionChange && field ? (
                <ComboBox
                    items={items}
                    name="component"
                    onBlur={field.onBlur}
                    onChange={(item) =>
                        handleComponentDefinitionChange(item?.componentDefinition as ComponentDefinitionBasicModel)
                    }
                    value={field.value}
                />
            ) : (
                <>
                    {selectedComponentDefinition?.icon ? (
                        <div className="relative">
                            <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md  px-3">
                                <InlineSVG className="size-4" src={selectedComponentDefinition.icon} />
                            </span>

                            <Input
                                className={twMerge(selectedComponentDefinition?.icon && 'pl-10')}
                                disabled
                                value={componentDefinition?.title}
                            />
                        </div>
                    ) : (
                        <Input disabled value={componentDefinition?.title} />
                    )}
                </>
            )}
        </FormControl>

        <FormMessage />
    </FormItem>
);

export default ComponentSelectionInput;
