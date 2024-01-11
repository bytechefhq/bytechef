import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {
    IntegrationInstanceConfigurationModel,
    IntegrationModel,
    IntegrationStatusModel,
} from '@/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/queries/embedded/integrationInstanceConfigurationTags.queries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/platform/componentDefinitions.queries';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {Control, UseFormGetValues, UseFormRegister, UseFormReturn, UseFormSetValue} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

interface IntegrationInstanceConfigurationDialogBasicStepProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    errors: UseFormReturn<IntegrationInstanceConfigurationModel>['formState']['errors'];
    getValues: UseFormGetValues<IntegrationInstanceConfigurationModel>;
    integrationInstanceConfiguration: IntegrationInstanceConfigurationModel | undefined;
    register: UseFormRegister<IntegrationInstanceConfigurationModel>;
    setValue: UseFormSetValue<IntegrationInstanceConfigurationModel>;
    touchedFields: UseFormReturn<IntegrationInstanceConfigurationModel>['formState']['touchedFields'];
}

const IntegrationLabel = ({
    componentDefinition,
    integration,
}: {
    componentDefinition: ComponentDefinitionBasicModel;
    integration: IntegrationModel;
}) => (
    <div className="flex items-center gap-2">
        {componentDefinition?.icon && <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />}

        <span className="mr-1 ">{componentDefinition.title}</span>

        <span className="text-xs text-gray-500">{integration?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

const IntegrationInstanceConfigurationDialogBasicStep = ({
    control,
    getValues,
    integrationInstanceConfiguration,
    setValue,
}: IntegrationInstanceConfigurationDialogBasicStepProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsLoading,
    } = useGetIntegrationsQuery({status: IntegrationStatusModel.Published});

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationInstanceConfigurationTagsQuery();

    const tagNames = integrationInstanceConfiguration?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return (
        <>
            {integrationsError && !integrationsLoading && (
                <span>An error has occurred: {integrationsError.message}</span>
            )}

            {tagsError && !tagsLoading && <span>An error has occurred: {tagsError.message}</span>}

            {componentDefinitions && integrations ? (
                <div className="grid gap-4">
                    {!integrationInstanceConfiguration?.id && (
                        <FormField
                            control={control}
                            name="integrationId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Integration</FormLabel>

                                    <FormControl>
                                        <ComboBox
                                            items={integrations.map(
                                                (integration) =>
                                                    ({
                                                        label: (
                                                            <IntegrationLabel
                                                                componentDefinition={
                                                                    componentDefinitions.filter(
                                                                        (componentDefinition) =>
                                                                            componentDefinition.name ===
                                                                            integration.componentName
                                                                    )[0]
                                                                }
                                                                integration={integration}
                                                            />
                                                        ),
                                                        value: integration.id,
                                                    }) as ComboBoxItemType
                                            )}
                                            name="integrationId"
                                            onBlur={field.onBlur}
                                            onChange={(item) => {
                                                if (item) {
                                                    setValue('integrationId', item.value);
                                                }
                                            }}
                                            value={field.value}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                            shouldUnregister={false}
                        />
                    )}

                    <FormField
                        control={control}
                        name="environment"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Environment</FormLabel>

                                <FormControl>
                                    <Select defaultValue={field.value} onValueChange={field.onChange}>
                                        <SelectTrigger className="w-full">
                                            <SelectValue placeholder="Select environment" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectItem value="DEVELOPMENT">Development</SelectItem>

                                            <SelectItem value="STAGING">Staging</SelectItem>

                                            <SelectItem value="PRODUCTION">Production</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required: true}}
                        shouldUnregister={false}
                    />

                    <FormField
                        control={control}
                        name="description"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Description</FormLabel>

                                <FormControl>
                                    <Textarea placeholder="Cute description of your project instance" {...field} />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    {remainingTags && (
                        <FormField
                            control={control}
                            name="tags"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Tags</FormLabel>

                                    <CreatableSelect
                                        field={field}
                                        isMulti
                                        onCreateOption={(inputValue: string) => {
                                            setValue('tags', [
                                                ...getValues().tags!,
                                                {
                                                    label: inputValue,
                                                    name: inputValue,
                                                    value: inputValue,
                                                },
                                            ] as never[]);
                                        }}
                                        options={remainingTags.map((tag) => {
                                            return {
                                                label: tag.name,
                                                value: tag.name.toLowerCase().replace(/\W/g, ''),
                                                ...tag,
                                            };
                                        })}
                                    />

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    )}
                </div>
            ) : (
                <span className="px-2">Loading...</span>
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStep;
