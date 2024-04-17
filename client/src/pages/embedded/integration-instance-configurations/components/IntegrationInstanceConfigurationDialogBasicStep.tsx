import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {
    IntegrationInstanceConfigurationModel,
    IntegrationModel,
    IntegrationStatusModel,
    IntegrationVersionModel,
} from '@/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useGetIntegrationInstanceConfigurationTagsQuery} from '@/queries/embedded/integrationInstanceConfigurationTags.queries';
import {useGetIntegrationVersionsQuery} from '@/queries/embedded/integrationVersions.queries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/platform/componentDefinitions.queries';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {Dispatch, FocusEventHandler, SetStateAction} from 'react';
import {Control, UseFormGetValues, UseFormRegister, UseFormReturn, UseFormSetValue} from 'react-hook-form';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';
import InlineSVG from 'react-inlinesvg';

interface IntegrationInstanceConfigurationDialogBasicStepProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    errors: UseFormReturn<IntegrationInstanceConfigurationModel>['formState']['errors'];
    getValues: UseFormGetValues<IntegrationInstanceConfigurationModel>;
    integrationId?: number;
    integrationInstanceConfiguration: IntegrationInstanceConfigurationModel | undefined;
    register: UseFormRegister<IntegrationInstanceConfigurationModel>;
    setIntegrationId: Dispatch<SetStateAction<number | undefined>>;
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

const IntegrationsComboBox = ({
    onBlur,
    onChange,
    value,
}: {
    onBlur: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    value?: number;
}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {data: integrations} = useGetIntegrationsQuery();

    return integrations && componentDefinitions ? (
        <ComboBox
            items={integrations.map(
                (integration) =>
                    ({
                        label: (
                            <IntegrationLabel
                                componentDefinition={
                                    componentDefinitions.filter(
                                        (componentDefinition) => componentDefinition.name === integration.componentName
                                    )[0]
                                }
                                integration={integration}
                            />
                        ),
                        value: integration.id,
                    }) as ComboBoxItemType
            )}
            name="integrationId"
            onBlur={onBlur}
            onChange={onChange}
            value={value}
        />
    ) : (
        <>Loading...</>
    );
};

const IntegrationVersionsSelect = ({
    disabled,
    integrationVersion,
    integrationVersions,
    onChange,
}: {
    disabled: boolean;
    onChange: (value: number) => void;
    integrationVersion?: number;
    integrationVersions: IntegrationVersionModel[];
}) => {
    return (
        <Select
            defaultValue={integrationVersion?.toString()}
            disabled={disabled}
            onValueChange={(value) => {
                onChange(+value);
            }}
        >
            <SelectTrigger className="w-full">
                <SelectValue placeholder="Select version" />
            </SelectTrigger>

            <SelectContent>
                {integrationVersions &&
                    integrationVersions.map((integrationVersion) => (
                        <SelectItem key={integrationVersion.version} value={integrationVersion.version!.toString()}>
                            V{integrationVersion.version}
                        </SelectItem>
                    ))}
            </SelectContent>
        </Select>
    );
};

const TagsSelect = ({
    field,
    integrationInstanceConfiguration,
    onCreateOption,
}: {
    field: ControllerRenderProps<IntegrationInstanceConfigurationModel, 'tags'>;
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationModel;
    onCreateOption: (inputValue: string) => void;
}) => {
    const {data: tags} = useGetIntegrationInstanceConfigurationTagsQuery();

    const tagNames = integrationInstanceConfiguration?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return remainingTags ? (
        <CreatableSelect
            field={field}
            isMulti
            onCreateOption={onCreateOption}
            options={remainingTags.map((tag) => {
                return {
                    label: tag.name,
                    value: tag.name.toLowerCase().replace(/\W/g, ''),
                    ...tag,
                };
            })}
        />
    ) : (
        <>Loading...</>
    );
};

const IntegrationInstanceConfigurationDialogBasicStep = ({
    control,
    getValues,
    integrationId,
    integrationInstanceConfiguration,
    setIntegrationId,
    setValue,
}: IntegrationInstanceConfigurationDialogBasicStepProps) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId!, !!integrationId);

    const filteredIntegrationVersions = integrationVersions?.filter(
        (integrationVersion) => integrationVersion.status === IntegrationStatusModel.Published
    );

    return (
        <div className="grid gap-4">
            {!integrationInstanceConfiguration?.id && (
                <FormField
                    control={control}
                    name="integrationId"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Integration</FormLabel>

                            <FormControl>
                                <IntegrationsComboBox
                                    onBlur={field.onBlur}
                                    onChange={(item) => {
                                        if (item) {
                                            setValue('integrationId', item.value);

                                            setIntegrationId(item.value);
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

            {!integrationInstanceConfiguration?.id &&
                filteredIntegrationVersions &&
                filteredIntegrationVersions.length > 0 && (
                    <FormField
                        control={control}
                        name="integrationVersion"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Version</FormLabel>

                                <FormControl>
                                    <IntegrationVersionsSelect
                                        disabled={!!integrationInstanceConfiguration?.integrationVersion}
                                        integrationVersion={field.value}
                                        integrationVersions={filteredIntegrationVersions}
                                        onChange={(value) => field.onChange(value)}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        shouldUnregister={false}
                    />
                )}

            {!integrationInstanceConfiguration?.id && (
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
            )}

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

            <FormField
                control={control}
                name="tags"
                render={({field}) => (
                    <FormItem>
                        <FormLabel>Tags</FormLabel>

                        <TagsSelect
                            field={field}
                            integrationInstanceConfiguration={integrationInstanceConfiguration}
                            onCreateOption={(inputValue: string) => {
                                setValue('tags', [
                                    ...(getValues().tags ?? []),
                                    {
                                        label: inputValue,
                                        name: inputValue,
                                        value: inputValue,
                                    },
                                ] as never[]);
                            }}
                        />

                        <FormMessage />
                    </FormItem>
                )}
            />
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStep;
