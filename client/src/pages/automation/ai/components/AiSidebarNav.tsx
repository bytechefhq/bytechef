import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useShallow} from 'zustand/react/shallow';

const AI_BASE_PATH = '/automation/ai';

interface SectionDefinitionI {
    id: string;
    label: string;
}

const SKILLS_SECTIONS: SectionDefinitionI[] = [
    {id: 'skills', label: 'Skills'},
    {id: 'memories', label: 'Memories'},
];

const GATEWAY_SECTIONS: SectionDefinitionI[] = [
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

const skillsPath = (id: string) => `${AI_BASE_PATH}/${id}`;

const gatewayPath = (id: string) => `${AI_BASE_PATH}/gateway?section=${id}`;

interface AiSidebarNavPropsI {
    currentSection: string;
}

const AiSidebarNav = ({currentSection}: AiSidebarNavPropsI) => {
    const {edition, gatewayEnabled} = useApplicationInfoStore(
        useShallow((state) => ({
            edition: state.application?.edition,
            gatewayEnabled: state.ai.gateway.enabled,
        }))
    );

    const showGateway = edition === EditionType.EE && gatewayEnabled;

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        {SKILLS_SECTIONS.map((section) => (
                            <LeftSidebarNavItem
                                item={{current: currentSection === section.id, name: section.label}}
                                key={section.id}
                                toLink={skillsPath(section.id)}
                            />
                        ))}
                    </>
                }
            />

            {showGateway && (
                <LeftSidebarNav
                    body={
                        <>
                            {GATEWAY_SECTIONS.map((section) => (
                                <LeftSidebarNavItem
                                    item={{current: currentSection === section.id, name: section.label}}
                                    key={section.id}
                                    toLink={gatewayPath(section.id)}
                                />
                            ))}
                        </>
                    }
                    title="Gateway"
                />
            )}
        </>
    );
};

export default AiSidebarNav;
