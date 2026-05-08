import AiSidebarNav from '@/pages/automation/ai/components/AiSidebarNav';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useSearchParams} from 'react-router-dom';

import AiObservabilityAlerts from './components/alerts/AiObservabilityAlerts';
import AiGatewayBudget from './components/budget/AiGatewayBudget';
import AiDatasets from './components/datasets/AiDatasets';
import AiExperiments from './components/experiments/AiExperiments';
import AiObservabilityExports from './components/exports/AiObservabilityExports';
import AiGatewayModels from './components/models/AiGatewayModels';
import AiGatewayDashboard from './components/monitoring/AiGatewayDashboard';
import AiGatewayPlayground from './components/playground/AiGatewayPlayground';
import AiGatewayProjects from './components/projects/AiGatewayProjects';
import AiPrompts from './components/prompts/AiPrompts';
import AiGatewayProviders from './components/providers/AiGatewayProviders';
import AiGatewayRateLimits from './components/rate-limits/AiGatewayRateLimits';
import AiGatewayRoutingPolicies from './components/routing/AiGatewayRoutingPolicies';
import AiEvalScores from './components/scores/AiEvalScores';
import AiObservabilitySessionDetail from './components/sessions/AiObservabilitySessionDetail';
import AiObservabilitySessions from './components/sessions/AiObservabilitySessions';
import AiGatewaySettings from './components/settings/AiGatewaySettings';
import AiObservabilityTraceDetail from './components/traces/AiObservabilityTraceDetail';
import AiObservabilityTraces from './components/traces/AiObservabilityTraces';

type AiGatewayPageType =
    | 'alerts'
    | 'budget'
    | 'datasets'
    | 'experiments'
    | 'exports'
    | 'models'
    | 'monitoring'
    | 'playground'
    | 'projects'
    | 'prompts'
    | 'providers'
    | 'rateLimits'
    | 'routing'
    | 'scores'
    | 'sessions'
    | 'settings'
    | 'traces';

const VALID_PAGES: ReadonlySet<AiGatewayPageType> = new Set([
    'alerts',
    'budget',
    'datasets',
    'experiments',
    'exports',
    'models',
    'monitoring',
    'playground',
    'projects',
    'prompts',
    'providers',
    'rateLimits',
    'routing',
    'scores',
    'sessions',
    'settings',
    'traces',
]);

const isValidPage = (value: string | null): value is AiGatewayPageType =>
    value !== null && VALID_PAGES.has(value as AiGatewayPageType);

const AiGateway = () => {
    const [searchParams, setSearchParams] = useSearchParams();

    const sectionParam = searchParams.get('section');
    const activePage: AiGatewayPageType = isValidPage(sectionParam) ? sectionParam : 'providers';

    const selectedSessionId = searchParams.get('sessionId') ?? undefined;
    const selectedTraceId = searchParams.get('traceId') ?? undefined;

    const updateQueryParam = (key: string, value: string | undefined) => {
        setSearchParams(
            (current) => {
                const next = new URLSearchParams(current);

                if (value === undefined) {
                    next.delete(key);
                } else {
                    next.set(key, value);
                }

                return next;
            },
            {replace: true}
        );
    };

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="LLM Gateway" />}
            leftSidebarBody={<AiSidebarNav currentSection={activePage} />}
            leftSidebarHeader={<Header position="sidebar" title="AI" />}
            leftSidebarWidth="64"
        >
            {activePage === 'providers' && <AiGatewayProviders />}

            {activePage === 'models' && <AiGatewayModels />}

            {activePage === 'projects' && <AiGatewayProjects />}

            {activePage === 'routing' && <AiGatewayRoutingPolicies />}

            {activePage === 'prompts' && <AiPrompts />}

            {activePage === 'settings' && <AiGatewaySettings />}

            {activePage === 'budget' && <AiGatewayBudget />}

            {activePage === 'rateLimits' && <AiGatewayRateLimits />}

            {activePage === 'monitoring' && <AiGatewayDashboard />}

            {activePage === 'playground' && <AiGatewayPlayground />}

            {activePage === 'traces' &&
                (selectedTraceId ? (
                    <AiObservabilityTraceDetail
                        onBack={() => updateQueryParam('traceId', undefined)}
                        traceId={selectedTraceId}
                    />
                ) : (
                    <AiObservabilityTraces onSelectTrace={(traceId) => updateQueryParam('traceId', traceId)} />
                ))}

            {activePage === 'scores' && <AiEvalScores />}

            {activePage === 'datasets' && <AiDatasets />}

            {activePage === 'experiments' && <AiExperiments />}

            {activePage === 'alerts' && <AiObservabilityAlerts />}

            {activePage === 'exports' && <AiObservabilityExports />}

            {activePage === 'sessions' &&
                (selectedSessionId ? (
                    <AiObservabilitySessionDetail
                        onBack={() => updateQueryParam('sessionId', undefined)}
                        sessionId={selectedSessionId}
                    />
                ) : (
                    <AiObservabilitySessions
                        onSelectSession={(sessionId) => updateQueryParam('sessionId', sessionId)}
                    />
                ))}
        </LayoutContainer>
    );
};

export default AiGateway;
