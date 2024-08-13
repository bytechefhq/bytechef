import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox';
import IntegrationInstanceConfigurationDialogBasicStepTagsSelect from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogBasicStepTagsSelect';
import {useWorkflowsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {IntegrationInstanceConfigurationModel} from '@/shared/middleware/embedded/configuration';
import {Dispatch, SetStateAction} from 'react';
import {Control, UseFormGetValues, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

interface IntegrationInstanceConfigurationDialogBasicStepProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    curIntegrationId?: number;
    curIntegrationVersion?: number;
    getValues: UseFormGetValues<IntegrationInstanceConfigurationModel>;
    integrationInstanceConfiguration: IntegrationInstanceConfigurationModel | undefined;
    setCurIntegrationId: Dispatch<SetStateAction<number | undefined>>;
    setCurIntegrationVersion: Dispatch<SetStateAction<number | undefined>>;
    setValue: UseFormSetValue<IntegrationInstanceConfigurationModel>;
}

const IntegrationInstanceConfigurationDialogBasicStep = ({
    control,
    curIntegrationId,
    curIntegrationVersion,
    getValues,
    integrationInstanceConfiguration,
    setCurIntegrationId,
    setCurIntegrationVersion,
    setValue,
}: IntegrationInstanceConfigurationDialogBasicStepProps) => {
    const [resetWorkflowsEnabledStore] = useWorkflowsEnabledStore(useShallow(({reset}) => [reset]));

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
                                <IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox
                                    onBlur={field.onBlur}
                                    onChange={(item) => {
                                        if (item) {
                                            resetWorkflowsEnabledStore();
                                            setValue('integrationId', item.value);
                                            setValue('integrationVersion', undefined);

                                            if (!getValues('name')) {
                                                setValue('name', item.name!.toString());
                                            }

                                            setCurIntegrationId(item.value);
                                            setCurIntegrationVersion(undefined);
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
                name="name"
                render={({field}) => (
                    <FormItem>
                        <FormLabel>Name</FormLabel>

                        <FormControl>
                            <Input placeholder="My Integration" {...field} />
                        </FormControl>

                        <FormMessage />
                    </FormItem>
                )}
                rules={{required: true}}
            />

            {!integrationInstanceConfiguration?.id && curIntegrationId && (
                <FormField
                    control={control}
                    name="integrationVersion"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>Version</FormLabel>

                            <FormControl>
                                <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                                    integrationId={curIntegrationId}
                                    integrationVersion={curIntegrationVersion}
                                    onChange={(value) => {
                                        field.onChange(value);
                                        setValue('integrationInstanceConfigurationWorkflows', []);
                                        setCurIntegrationVersion(value);
                                    }}
                                />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: true}}
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
                                <Select defaultValue={field.value} onValueChange={(value) => field.onChange(value)}>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="Select environment" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="TEST">Test</SelectItem>

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
                            <Textarea
                                placeholder="Cute description of your integration instance configuration"
                                {...field}
                            />
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

                        <IntegrationInstanceConfigurationDialogBasicStepTagsSelect
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
