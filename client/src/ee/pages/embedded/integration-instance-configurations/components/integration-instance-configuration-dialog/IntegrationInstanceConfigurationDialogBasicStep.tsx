import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox';
import IntegrationInstanceConfigurationDialogBasicStepTagsSelect from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepTagsSelect';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Dispatch, SetStateAction} from 'react';
import {Control, UseFormGetValues, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

interface IntegrationInstanceConfigurationDialogBasicStepProps {
    control: Control<IntegrationInstanceConfiguration>;
    curIntegrationId?: number;
    curIntegrationVersion?: number;
    getValues: UseFormGetValues<IntegrationInstanceConfiguration>;
    integrationInstanceConfiguration: IntegrationInstanceConfiguration | undefined;
    setCurIntegrationId: Dispatch<SetStateAction<number | undefined>>;
    setCurIntegrationVersion: Dispatch<SetStateAction<number | undefined>>;
    setValue: UseFormSetValue<IntegrationInstanceConfiguration>;
    updateIntegrationVersion?: boolean;
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
    updateIntegrationVersion = false,
}: IntegrationInstanceConfigurationDialogBasicStepProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
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

            {!updateIntegrationVersion && (
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
            )}

            {curIntegrationId && (!integrationInstanceConfiguration?.id || updateIntegrationVersion) && (
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

            <FormField
                control={control}
                name="environmentId"
                render={() => (
                    <FormItem className="space-x-2">
                        <FormLabel>Environment</FormLabel>

                        <FormControl>
                            <EnvironmentBadge environmentId={currentEnvironmentId} />
                        </FormControl>

                        <FormMessage />
                    </FormItem>
                )}
            />

            {!updateIntegrationVersion && (
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
            )}

            {!updateIntegrationVersion && (
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
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStep;
