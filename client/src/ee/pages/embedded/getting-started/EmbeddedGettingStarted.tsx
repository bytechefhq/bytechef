import GettingStartedHelpCard from '@/components/GettingStarted/GettingStartedHelpCard';
import GettingStartedNextSteps from '@/components/GettingStarted/GettingStartedNextSteps';
import StepDoneText from '@/components/GettingStarted/StepDoneText';
import JourneySteps, {JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import ConnectedUserSheet from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheet';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import buildFirstCallCurl from '@/ee/pages/embedded/getting-started/buildFirstCallCurl';
import ApiKeyStepAction from '@/ee/pages/embedded/getting-started/components/ApiKeyStepAction';
import EmbeddedGettingStartedDismissButton from '@/ee/pages/embedded/getting-started/components/EmbeddedGettingStartedDismissButton';
import FirstCallStepContent from '@/ee/pages/embedded/getting-started/components/FirstCallStepContent';
import IntegrationSubSteps from '@/ee/pages/embedded/getting-started/components/IntegrationSubSteps';
import TestUserStepContent from '@/ee/pages/embedded/getting-started/components/TestUserStepContent';
import TestUserSubSteps from '@/ee/pages/embedded/getting-started/components/TestUserSubSteps';
import {EMBEDDED_NEXT_STEPS} from '@/ee/pages/embedded/getting-started/embeddedNextSteps';
import {
    getEmbeddedGettingStartedStepStatuses,
    getIntegrationSubStage,
    isListeningForFirstCall,
} from '@/ee/pages/embedded/getting-started/getEmbeddedGettingStartedSteps';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Navigate} from 'react-router-dom';

const FIRST_CALL_POLLING_INTERVAL = 5000;

const INTRO = 'ByteChef lets your product run actions and tools in apps like GitHub and Gmail on behalf of your users.';

const DEVELOPMENT_ENVIRONMENT_NAME = ENVIRONMENT_CONFIGS[DEVELOPMENT_ENVIRONMENT].label;

const EmbeddedGettingStarted = () => {
    const [stepExpandedOverrides, setStepExpandedOverrides] = useState<Record<string, boolean>>({});

    const completedRef = useRef(false);

    const connectedUserSheetOpen = useConnectedUserSheetStore((state) => state.connectedUserSheetOpen);
    const setConnectedUserSheetOpen = useConnectedUserSheetStore((state) => state.setConnectedUserSheetOpen);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const isDevelopment = currentEnvironmentId === DEVELOPMENT_ENVIRONMENT;

    const {data, error, isLoading} = useEmbeddedGettingStartedQuery(
        {},
        {
            enabled: isDevelopment,
            refetchInterval: (query) => {
                const status = query.state.data?.embeddedGettingStarted;

                return status && isListeningForFirstCall(status) ? FIRST_CALL_POLLING_INTERVAL : false;
            },
        }
    );

    const status = data?.embeddedGettingStarted;

    const toggleStep = useCallback((stepId: string, expanded: boolean) => {
        setStepExpandedOverrides((previousOverrides) => ({...previousOverrides, [stepId]: !expanded}));
    }, []);

    const steps = useMemo<JourneyStepI[]>(() => {
        if (!status) {
            return [];
        }

        const stepStatuses = getEmbeddedGettingStartedStepStatuses(status);
        const integrationSubStage = getIntegrationSubStage(status);
        const testUserExternalId = status.testUserExternalId || undefined;

        const curl = buildFirstCallCurl({
            apiBaseUrl: status.apiBaseUrl || `${window.location.origin}/api/embedded/v1`,
            componentName: status.componentName || undefined,
            componentVersion: status.componentVersion || undefined,
            environmentName: DEVELOPMENT_ENVIRONMENT_NAME,
            externalUserId: testUserExternalId,
        });

        const firstCallDescription =
            status.componentName === 'httpClient'
                ? 'Set BYTECHEF_API_KEY to your API key, then run this in a terminal.'
                : 'Set BYTECHEF_API_KEY to your API key, replace ACTION_NAME and the input with an action of your integration, then run this in a terminal.';

        const setupStepsDone =
            stepStatuses.apiKey === 'complete' &&
            stepStatuses.integration === 'complete' &&
            stepStatuses.testUser === 'complete';

        const integrationDone = integrationSubStage === 'done';
        const integrationExpanded = stepExpandedOverrides.integration ?? !integrationDone;
        const testUserExpanded = stepExpandedOverrides.testUser ?? !status.testUserConnected;
        const firstCallExpanded = stepExpandedOverrides.firstCall ?? !status.firstCallMade;

        return [
            {
                action: <ApiKeyStepAction apiKeyCreated={status.apiKeyCreated} />,
                description: 'Your backend uses it to call the Embedded API.',
                id: 'apiKey',
                label: 'Create an API key',
                status: stepStatuses.apiKey,
            },
            {
                action: integrationDone && <StepDoneText text="Published and enabled" />,
                content: integrationExpanded && <IntegrationSubSteps status={status} />,
                description: 'An integration is the app your users connect, like GitHub or Gmail.',
                expanded: integrationExpanded,
                id: 'integration',
                label: 'Set up your first integration',
                onToggle: () => toggleStep('integration', integrationExpanded),
                status: stepStatuses.integration,
            },
            {
                action: status.testUserConnected && <StepDoneText text="User connected" />,
                content: testUserExpanded && (
                    <div className="space-y-3">
                        <TestUserSubSteps status={status} />

                        {status.testUserConnected && testUserExternalId && (
                            <TestUserStepContent externalUserId={testUserExternalId} />
                        )}
                    </div>
                ),
                description: "A connected user stands in for one of your app's users while you test.",
                expanded: testUserExpanded,
                id: 'testUser',
                label: 'Connect a test user',
                onToggle: () => toggleStep('testUser', testUserExpanded),
                status: stepStatuses.testUser,
            },
            {
                action: status.firstCallMade && <StepDoneText text="Call received" />,
                content: firstCallExpanded && (
                    <FirstCallStepContent
                        callReceived={status.firstCallMade}
                        curl={curl}
                        lastFailedCall={status.lastFailedCall}
                        listening={isListeningForFirstCall(status)}
                        muted={!setupStepsDone}
                    />
                ),
                description: firstCallDescription,
                expanded: firstCallExpanded,
                id: 'firstCall',
                label: 'Make your first call',
                onToggle: () => toggleStep('firstCall', firstCallExpanded),
                status: stepStatuses.firstCall,
            },
        ];
    }, [status, stepExpandedOverrides, toggleStep]);

    const completed = !!status?.completed;

    useEffect(() => {
        completedRef.current = completed;
    }, [completed]);

    useEffect(() => {
        if (!connectedUserSheetOpen) {
            void queryClient.invalidateQueries({queryKey: ['embeddedGettingStarted']});
        }
    }, [connectedUserSheetOpen, queryClient]);

    useEffect(() => () => setConnectedUserSheetOpen(false), [setConnectedUserSheetOpen]);

    useEffect(
        () => () => {
            if (completedRef.current) {
                void queryClient.invalidateQueries({queryKey: ['embeddedGettingStartedCompleted']});
            }
        },
        [queryClient]
    );

    if (!isDevelopment) {
        return <Navigate replace to="/embedded/configurations" />;
    }

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description={INTRO}
                        position="main"
                        right={status && !completed && <EmbeddedGettingStartedDismissButton />}
                        title={status?.firstCallMade ? "You're set up" : 'Getting Started'}
                    />
                }
                leftSidebarOpen={false}
            >
                <div className="mx-auto h-fit w-full max-w-3xl space-y-6 px-4 pt-4 pb-12">
                    <JourneySteps aria-label="Setup steps" orientation="vertical" steps={steps} />

                    <hr className="border-stroke-neutral-secondary" />

                    <GettingStartedNextSteps steps={EMBEDDED_NEXT_STEPS} />

                    <GettingStartedHelpCard title="Need help integrating ByteChef?" />
                </div>

                <ConnectedUserSheet />
            </LayoutContainer>
        </PageLoader>
    );
};

export default EmbeddedGettingStarted;
