import SubStepList from '@/components/GettingStarted/SubStepList';
import {getIntegrationSubSteps} from '@/ee/pages/embedded/getting-started/getEmbeddedGettingStartedSubSteps';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationPublishDialog from '@/ee/pages/embedded/integrations/components/IntegrationPublishDialog';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {useEnableIntegrationInstanceConfigurationMutation} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const STARTER_INTEGRATION = {componentName: 'httpClient', name: 'HTTP Client'} as Integration;

type IntegrationDialogType = 'configure' | 'create' | 'publish';

interface IntegrationSubStepsProps {
    status: EmbeddedGettingStartedQuery['embeddedGettingStarted'];
}

const IntegrationSubSteps = ({status}: IntegrationSubStepsProps) => {
    const [openDialog, setOpenDialog] = useState<IntegrationDialogType | null>(null);

    const {captureIntegrationInstanceConfigurationEnabled} = useAnalytics();

    const queryClient = useQueryClient();

    const {data: integration} = useGetIntegrationQuery(
        Number(status.integrationId),
        undefined,
        openDialog === 'publish' && status.integrationId != null
    );

    const enableIntegrationInstanceConfigurationMutation = useEnableIntegrationInstanceConfigurationMutation({
        onSuccess: () => {
            captureIntegrationInstanceConfigurationEnabled();

            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
            });
            void queryClient.invalidateQueries({queryKey: ['embeddedGettingStarted']});
        },
    });

    const integrationId = status.integrationId != null ? Number(status.integrationId) : undefined;
    const integrationInstanceConfigurationId =
        status.integrationInstanceConfigurationId != null
            ? Number(status.integrationInstanceConfigurationId)
            : undefined;

    const closeDialog = () => {
        setOpenDialog(null);

        void queryClient.invalidateQueries({queryKey: ['embeddedGettingStarted']});
    };

    const subStepActions: Partial<Record<string, () => void>> = {
        create: () => setOpenDialog('create'),
        ...(integrationId !== undefined && {publish: () => setOpenDialog('publish')}),
        ...(integrationId !== undefined &&
            (status.integrationPublished || !status.integrationHasWorkflows) && {
                configure: () => setOpenDialog('configure'),
            }),
        ...(integrationInstanceConfigurationId !== undefined && {
            enable: () => {
                if (enableIntegrationInstanceConfigurationMutation.isPending) {
                    return;
                }

                enableIntegrationInstanceConfigurationMutation.mutate({
                    enable: true,
                    id: integrationInstanceConfigurationId,
                });
            },
        }),
    };

    return (
        <>
            <SubStepList
                ariaLabel="Integration setup"
                subStepActions={subStepActions}
                subSteps={getIntegrationSubSteps(status)}
            />

            {openDialog === 'create' && <IntegrationDialog integration={STARTER_INTEGRATION} onClose={closeDialog} />}

            {openDialog === 'publish' && integration && (
                <IntegrationPublishDialog integration={integration} onClose={closeDialog} />
            )}

            {openDialog === 'configure' && (
                <IntegrationInstanceConfigurationDialog
                    integrationInstanceConfiguration={
                        {
                            environmentId: DEVELOPMENT_ENVIRONMENT,
                            integrationId,
                        } as IntegrationInstanceConfiguration
                    }
                    onClose={closeDialog}
                />
            )}
        </>
    );
};

export default IntegrationSubSteps;
