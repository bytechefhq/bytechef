import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useState} from 'react';

import AiObservabilityAlerts from './components/alerts/AiObservabilityAlerts';
import AiGatewayBudget from './components/budget/AiGatewayBudget';
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
import AiGatewayTags from './components/tags/AiGatewayTags';
import AiObservabilityTraceDetail from './components/traces/AiObservabilityTraceDetail';
import AiObservabilityTraces from './components/traces/AiObservabilityTraces';

type AiGatewayPageType =
    | 'alerts'
    | 'budget'
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
    | 'tags'
    | 'traces';

const AiGateway = () => {
    const [activePage, setActivePage] = useState<AiGatewayPageType>('providers');
    const [selectedSessionId, setSelectedSessionId] = useState<string | undefined>(undefined);
    const [selectedTraceId, setSelectedTraceId] = useState<string | undefined>(undefined);

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="LLM Gateway" />}
            leftSidebarBody={
                <LeftSidebarNav
                    body={
                        <>
                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'providers',
                                    name: 'Providers',
                                    onItemClick: () => setActivePage('providers'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'models',
                                    name: 'Models',
                                    onItemClick: () => setActivePage('models'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'projects',
                                    name: 'Projects',
                                    onItemClick: () => setActivePage('projects'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'tags',
                                    name: 'Tags',
                                    onItemClick: () => setActivePage('tags'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'routing',
                                    name: 'Routing Policies',
                                    onItemClick: () => setActivePage('routing'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'prompts',
                                    name: 'Prompts',
                                    onItemClick: () => setActivePage('prompts'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'settings',
                                    name: 'Settings',
                                    onItemClick: () => setActivePage('settings'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'budget',
                                    name: 'Budget',
                                    onItemClick: () => setActivePage('budget'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'rateLimits',
                                    name: 'Rate Limits',
                                    onItemClick: () => setActivePage('rateLimits'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'monitoring',
                                    name: 'Monitoring',
                                    onItemClick: () => setActivePage('monitoring'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'playground',
                                    name: 'Playground',
                                    onItemClick: () => setActivePage('playground'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'traces',
                                    name: 'Traces',
                                    onItemClick: () => {
                                        setActivePage('traces');
                                        setSelectedTraceId(undefined);
                                    },
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'sessions',
                                    name: 'Sessions',
                                    onItemClick: () => {
                                        setActivePage('sessions');
                                        setSelectedSessionId(undefined);
                                    },
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'scores',
                                    name: 'Scores',
                                    onItemClick: () => setActivePage('scores'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'alerts',
                                    name: 'Alerts',
                                    onItemClick: () => setActivePage('alerts'),
                                }}
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: activePage === 'exports',
                                    name: 'Exports',
                                    onItemClick: () => setActivePage('exports'),
                                }}
                            />
                        </>
                    }
                    title="Configuration"
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="LLM Gateway" />}
            leftSidebarWidth="64"
        >
            {activePage === 'providers' && <AiGatewayProviders />}

            {activePage === 'models' && <AiGatewayModels />}

            {activePage === 'projects' && <AiGatewayProjects />}

            {activePage === 'tags' && <AiGatewayTags />}

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
                        onBack={() => setSelectedTraceId(undefined)}
                        traceId={selectedTraceId}
                    />
                ) : (
                    <AiObservabilityTraces onSelectTrace={setSelectedTraceId} />
                ))}

            {activePage === 'scores' && <AiEvalScores />}

            {activePage === 'alerts' && <AiObservabilityAlerts />}

            {activePage === 'exports' && <AiObservabilityExports />}

            {activePage === 'sessions' &&
                (selectedSessionId ? (
                    <AiObservabilitySessionDetail
                        onBack={() => setSelectedSessionId(undefined)}
                        sessionId={selectedSessionId}
                    />
                ) : (
                    <AiObservabilitySessions onSelectSession={setSelectedSessionId} />
                ))}
        </LayoutContainer>
    );
};

export default AiGateway;
