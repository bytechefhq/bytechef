import {Input} from '@/components/Input/Input';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox';
import IntegrationInstanceConfigurationDialogBasicStepTagsSelect from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepTagsSelect';
import {NO_AUTHORIZATION_TYPE} from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/noAuthorizationType';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {Authorization} from '@/shared/middleware/platform/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {InfoIcon} from 'lucide-react';
import {Dispatch, SetStateAction} from 'react';
import {Control, UseFormGetValues, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

interface IntegrationInstanceConfigurationDialogBasicStepProps {
    authorizationRequired?: boolean;
    authorizations?: Authorization[];
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
    authorizationRequired = true,
    authorizations,
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
                            <FormLabel className="gap-0">
                                Integration
                                <RequiredMark />
                            </FormLabel>

                            <FormControl>
                                <IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox
                                    disabled={!!integrationInstanceConfiguration?.integrationId}
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
                            <FormLabel className="gap-0">
                                Name
                                <RequiredMark />
                            </FormLabel>

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
                            <FormLabel className="gap-0">
                                Version
                                <RequiredMark />
                            </FormLabel>

                            <FormControl>
                                <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                                    excludedIntegrationVersion={
                                        updateIntegrationVersion
                                            ? integrationInstanceConfiguration?.integrationVersion
                                            : undefined
                                    }
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

            {curIntegrationId && authorizations && authorizations.length + (authorizationRequired ? 0 : 1) > 1 && (
                <FormField
                    control={control}
                    name="authorizationType"
                    render={({field}) => (
                        <FormItem>
                            <FormLabel className="gap-0">
                                Authorization
                                {authorizationRequired && <RequiredMark />}
                            </FormLabel>

                            <Select
                                disabled={!!integrationInstanceConfiguration?.id}
                                onValueChange={field.onChange}
                                value={
                                    field.value ||
                                    (integrationInstanceConfiguration?.id && !authorizationRequired
                                        ? NO_AUTHORIZATION_TYPE
                                        : undefined)
                                }
                            >
                                <FormControl>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="Select authorization" />
                                    </SelectTrigger>
                                </FormControl>

                                <SelectContent>
                                    {!authorizationRequired && (
                                        <SelectItem value={NO_AUTHORIZATION_TYPE}>None</SelectItem>
                                    )}

                                    {authorizations.map((authorization) => (
                                        <SelectItem key={authorization.type} value={authorization.type!}>
                                            {authorization.title || authorization.type}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>

                            <FormMessage />
                        </FormItem>
                    )}
                    rules={{required: authorizationRequired}}
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
                    name="permissionExpression"
                    render={({field}) => (
                        <FormItem>
                            <div className="flex items-center gap-1.5">
                                <FormLabel>Assignment rule</FormLabel>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <InfoIcon
                                            aria-label="About Assignment rule"
                                            className="size-4 text-content-neutral-secondary"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent className="max-w-80">
                                        Which connected users self-serve places on this configuration. Checked once,
                                        when their instance is created. Users already on it stay until you move them.
                                        Empty means everyone.
                                    </TooltipContent>
                                </Tooltip>
                            </div>

                            <FormControl>
                                <Input
                                    placeholder="e.g. metadata['beta'] == 'true'"
                                    {...field}
                                    value={field.value ?? ''}
                                />
                            </FormControl>

                            <FormMessage />
                        </FormItem>
                    )}
                />
            )}

            {!updateIntegrationVersion && currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT && (
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
