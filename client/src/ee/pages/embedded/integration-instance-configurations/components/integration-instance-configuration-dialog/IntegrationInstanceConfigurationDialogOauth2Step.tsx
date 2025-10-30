import {Label} from '@/components/ui/label';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {Authorization} from '@/shared/middleware/platform/configuration';
import {useGetOAuth2PropertiesQuery} from '@/shared/queries/platform/oauth2.queries';
import {Dispatch, SetStateAction} from 'react';
import {Control, FormState} from 'react-hook-form';

const IntegrationInstanceConfigurationDialogOauth2Step = ({
    componentName,
    control,
    formState,
    oAuth2Authorization,
    setUsePredefinedOAuthApp,
    usePredefinedOAuthApp,
}: {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    formState: FormState<IntegrationInstanceConfiguration>;
    oAuth2Authorization: Authorization;
    usePredefinedOAuthApp: boolean;
    setUsePredefinedOAuthApp: Dispatch<SetStateAction<boolean>>;
}) => {
    const {data: oAuth2Properties, isLoading: oAuth2PropertiesLoading} = useGetOAuth2PropertiesQuery();

    const showOAuth2AppPredefined =
        !oAuth2PropertiesLoading && oAuth2Properties?.predefinedApps?.includes(componentName);

    const showAuthorizationProperties = !showOAuth2AppPredefined || !usePredefinedOAuthApp;

    return (
        <div className="space-y-2">
            <Label>OAuth2 Credentials</Label>

            {showAuthorizationProperties && oAuth2Authorization.properties && (
                <Properties
                    control={control}
                    controlPath={'connectionParameters'}
                    formState={formState}
                    properties={oAuth2Authorization.properties}
                />
            )}

            {showOAuth2AppPredefined && (
                <div>
                    <a
                        className="text-sm text-blue-600"
                        href="#"
                        onClick={() => setUsePredefinedOAuthApp(!usePredefinedOAuthApp)}
                    >
                        {usePredefinedOAuthApp && <span>I want to use my own app credentials</span>}

                        {!usePredefinedOAuthApp && <span>I want to use predefined app credentials</span>}
                    </a>
                </div>
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogOauth2Step;
