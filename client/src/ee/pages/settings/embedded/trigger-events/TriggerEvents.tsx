import PageLoader from '@/components/PageLoader';
import TriggerEventsSigningSecret from '@/ee/pages/settings/embedded/trigger-events/components/TriggerEventsSigningSecret';
import TriggerEventsWebhookUrlForm from '@/ee/pages/settings/embedded/trigger-events/components/TriggerEventsWebhookUrlForm';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useTriggerSubscriptionWebhookSettingsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

const TriggerEvents = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const environmentId = `${currentEnvironmentId}`;

    const {
        data: webhookSettingsData,
        error: webhookSettingsError,
        isLoading: webhookSettingsLoading,
    } = useTriggerSubscriptionWebhookSettingsQuery({environmentId});

    const webhookSettings = webhookSettingsData?.triggerSubscriptionWebhookSettings;

    return (
        <PageLoader errors={[webhookSettingsError]} loading={webhookSettingsLoading}>
            <LayoutContainer
                header={<Header centerTitle position="main" title="Trigger Events" />}
                leftSidebarOpen={false}
            >
                <div className="flex w-full max-w-3xl flex-col gap-y-8 p-4">
                    <TriggerEventsWebhookUrlForm
                        environmentId={environmentId}
                        key={`${environmentId}-${webhookSettings?.webhookUrl || ''}`}
                        webhookUrl={webhookSettings?.webhookUrl || ''}
                    />

                    {webhookSettings?.webhookUrl ? (
                        <TriggerEventsSigningSecret
                            environmentId={environmentId}
                            key={environmentId}
                            signingSecret={webhookSettings.signingSecret || undefined}
                        />
                    ) : (
                        <p className="rounded-md border border-dashed border-stroke-neutral-secondary p-6 text-center text-sm text-content-neutral-secondary">
                            Set a webhook URL to receive trigger events for this environment.
                        </p>
                    )}
                </div>
            </LayoutContainer>
        </PageLoader>
    );
};

export default TriggerEvents;
