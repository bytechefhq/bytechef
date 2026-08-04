import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
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

// Sidebar order — also the source for the per-section page title in the content header.
const GATEWAY_SECTIONS: {id: AiGatewayPageType; label: string}[] = [
    {id: 'providers', label: 'Providers'},
    {id: 'models', label: 'Models'},
    {id: 'projects', label: 'Projects'},
    {id: 'routing', label: 'Routing Policies'},
    {id: 'prompts', label: 'Prompts'},
    {id: 'settings', label: 'Settings'},
    {id: 'budget', label: 'Budget'},
    {id: 'rateLimits', label: 'Rate Limits'},
    {id: 'monitoring', label: 'Monitoring'},
    {id: 'playground', label: 'Playground'},
    {id: 'datasets', label: 'Datasets'},
    {id: 'experiments', label: 'Experiments'},
    {id: 'traces', label: 'Traces'},
    {id: 'sessions', label: 'Sessions'},
    {id: 'scores', label: 'Scores'},
    {id: 'alerts', label: 'Alerts'},
    {id: 'exports', label: 'Exports'},
];

const VALID_PAGES: ReadonlySet<AiGatewayPageType> = new Set(GATEWAY_SECTIONS.map((section) => section.id));

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

    const activePageLabel = GATEWAY_SECTIONS.find((section) => section.id === activePage)?.label ?? 'AI Gateway';

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title={activePageLabel} />}
            leftSidebarBody={
                <LeftSidebarNav
                    body={GATEWAY_SECTIONS.map((section) => (
                        <LeftSidebarNavItem
                            item={{current: activePage === section.id, name: section.label}}
                            key={section.id}
                            toLink={`/automation/ai/gateway?section=${section.id}`}
                        />
                    ))}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="AI Gateway" />}
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
