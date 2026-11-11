import {Label} from '@/components/ui/label';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Authorization, ConnectionDefinition} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {useMemo} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';

interface IntegrationInstanceConfigurationDialogCredentialsProps {
    authorization?: Authorization;
    connectionDefinition: ConnectionDefinition;
    control: Control<FieldValues>;
    formState: FormState<FieldValues>;
    sharedParameterNames: string[];
}

const IntegrationInstanceConfigurationDialogCredentials = ({
    authorization,
    connectionDefinition,
    control,
    formState,
    sharedParameterNames,
}: IntegrationInstanceConfigurationDialogCredentialsProps) => {
    const properties = useMemo(
        () =>
            [...(connectionDefinition.properties ?? []), ...(authorization?.properties ?? [])].filter((property) =>
                sharedParameterNames.includes(property.name!)
            ) as PropertyAllType[],
        [authorization, connectionDefinition, sharedParameterNames]
    );

    return (
        <fieldset className="space-y-2 border-0">
            <Label>New credentials</Label>

            <p className="text-sm text-content-neutral-secondary">
                Fields left empty keep their current value. The new values are also written to the connection of every
                connected user on this configuration. Their own tokens are kept.
            </p>

            <WorkflowMockProvider>
                <Properties control={control} controlPath="parameters" formState={formState} properties={properties} />
            </WorkflowMockProvider>
        </fieldset>
    );
};

export default IntegrationInstanceConfigurationDialogCredentials;
