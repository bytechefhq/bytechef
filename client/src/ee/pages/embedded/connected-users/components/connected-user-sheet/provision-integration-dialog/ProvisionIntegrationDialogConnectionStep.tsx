import {Label} from '@/components/ui/label';
import {ProvisionableIntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import OAuth2Button from '@/shared/components/connection/OAuth2Button';
import {CodePayloadI} from '@/shared/components/connection/oauth2/useOAuth2';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {PropertyAllType} from '@/shared/types';
import {Control, FieldValues, FormState} from 'react-hook-form';

interface ProvisionIntegrationDialogConnectionStepProps {
    componentName: string;
    componentVersion?: number;
    configuration: ProvisionableIntegrationInstanceConfiguration;
    control: Control<FieldValues>;
    formState: FormState<FieldValues>;
    onCodeSuccess: (payload: CodePayloadI) => void;
}

export const isOAuth2CodeAuthorizationType = (authorizationType?: string): boolean =>
    !!authorizationType?.startsWith('OAUTH2_AUTHORIZATION_CODE');

export const isOAuth2AppMisconfigured = (configuration?: ProvisionableIntegrationInstanceConfiguration): boolean =>
    isOAuth2CodeAuthorizationType(configuration?.integrationInstanceConfiguration?.authorizationType) &&
    (!configuration?.oAuth2AuthorizationParameters?.authorizationUrl ||
        !configuration.oAuth2AuthorizationParameters.clientId);

const ProvisionIntegrationDialogConnectionStep = ({
    componentName,
    componentVersion,
    configuration,
    control,
    formState,
    onCodeSuccess,
}: ProvisionIntegrationDialogConnectionStepProps) => {
    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({componentName, componentVersion});

    const authorizationType = configuration.integrationInstanceConfiguration?.authorizationType;
    const sharedConnectionParameterNames = configuration.sharedConnectionParameterNames ?? [];

    const authorization = connectionDefinition?.authorizations?.find(
        (currentAuthorization) => currentAuthorization.type === authorizationType
    );

    const authorizationProperties = authorization?.properties ?? [];

    const authorizationPropertyNames = new Set(authorizationProperties.map((property) => property.name));

    const connectionProperties = (connectionDefinition?.properties ?? []).filter(
        (property) => !authorizationPropertyNames.has(property.name)
    );

    const properties = [...connectionProperties, ...authorizationProperties].filter(
        (property) => !sharedConnectionParameterNames.includes(property.name!)
    ) as PropertyAllType[];

    const oAuth2AuthorizationParameters = configuration.oAuth2AuthorizationParameters;

    return (
        <div className="space-y-4">
            {properties.length > 0 && (
                <div className="space-y-2">
                    <Label>Connection</Label>

                    <WorkflowMockProvider>
                        <Properties
                            control={control}
                            controlPath="connectionParameters"
                            formState={formState}
                            properties={properties}
                        />
                    </WorkflowMockProvider>
                </div>
            )}

            {isOAuth2AppMisconfigured(configuration) && (
                <p className="text-sm text-destructive">
                    This configuration&apos;s OAuth2 app is missing its authorization URL or client ID.
                </p>
            )}

            {isOAuth2CodeAuthorizationType(authorizationType) &&
                oAuth2AuthorizationParameters?.authorizationUrl &&
                oAuth2AuthorizationParameters.clientId && (
                    <OAuth2Button
                        authorizationUrl={oAuth2AuthorizationParameters.authorizationUrl}
                        clientId={oAuth2AuthorizationParameters.clientId}
                        componentName={componentName}
                        extraQueryParameters={oAuth2AuthorizationParameters.extraQueryParameters}
                        onClick={(getAuth: () => void) => getAuth()}
                        onCodeSuccess={onCodeSuccess}
                        redirectUri={configuration.redirectUri ?? ''}
                        responseType="code"
                        scopes={oAuth2AuthorizationParameters.scopes}
                    />
                )}
        </div>
    );
};

export default ProvisionIntegrationDialogConnectionStep;
