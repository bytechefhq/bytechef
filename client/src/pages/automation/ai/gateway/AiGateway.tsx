import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useSearchParams} from 'react-router-dom';

import AiGatewayBudget from './components/budget/AiGatewayBudget';
import AiGatewayModels from './components/models/AiGatewayModels';
import AiGatewayDashboard from './components/monitoring/AiGatewayDashboard';
import AiGatewayProjects from './components/projects/AiGatewayProjects';
import AiGatewayProviders from './components/providers/AiGatewayProviders';
import AiGatewayRateLimits from './components/rate-limits/AiGatewayRateLimits';
import AiGatewayRoutingPolicies from './components/routing/AiGatewayRoutingPolicies';
import AiGatewaySettings from './components/settings/AiGatewaySettings';

type AiGatewayPageType =
    'budget' | 'models' | 'monitoring' | 'projects' | 'providers' | 'rateLimits' | 'routing' | 'settings';

const GATEWAY_SECTIONS: {id: AiGatewayPageType; label: string}[] = [
    {id: 'providers', label: 'Providers'},
    {id: 'models', label: 'Models'},
    {id: 'projects', label: 'Projects'},
    {id: 'routing', label: 'Routing Policies'},
    {id: 'settings', label: 'Settings'},
    {id: 'budget', label: 'Budget'},
    {id: 'rateLimits', label: 'Rate Limits'},
    {id: 'monitoring', label: 'Monitoring'},
];

const VALID_PAGES: ReadonlySet<AiGatewayPageType> = new Set(GATEWAY_SECTIONS.map((section) => section.id));

const isValidPage = (value: string | null): value is AiGatewayPageType =>
    value !== null && VALID_PAGES.has(value as AiGatewayPageType);

const AiGateway = () => {
    const [searchParams] = useSearchParams();

    const sectionParam = searchParams.get('section');
    const activePage: AiGatewayPageType = isValidPage(sectionParam) ? sectionParam : 'providers';

    const activePageLabel = GATEWAY_SECTIONS.find((section) => section.id === activePage)?.label || 'AI Gateway';

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

            {activePage === 'settings' && <AiGatewaySettings />}

            {activePage === 'budget' && <AiGatewayBudget />}

            {activePage === 'rateLimits' && <AiGatewayRateLimits />}

            {activePage === 'monitoring' && <AiGatewayDashboard />}
        </LayoutContainer>
    );
};

export default AiGateway;
