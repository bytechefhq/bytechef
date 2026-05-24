import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiSidebarNav from '@/pages/automation/ai/components/AiSidebarNav';
import AiSkillsPanel from '@/pages/automation/ai/skills/AiSkillsPanel';
import AiSkillsCreateDropdown from '@/pages/automation/ai/skills/components/AiSkillsCreateDropdown';
import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SearchIcon, SparklesIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';

type AiSkillsRouteType = 'detail' | 'list' | 'uploadForm' | 'writeForm';

const SKILLS_BASE_PATH = '/automation/ai/skills';

const determineRoute = (pathname: string, skillId: string | undefined): AiSkillsRouteType => {
    if (pathname.endsWith('/create/write')) {
        return 'writeForm';
    }

    if (pathname.endsWith('/create/upload')) {
        return 'uploadForm';
    }

    if (skillId) {
        return 'detail';
    }

    return 'list';
};

const AiSkills = () => {
    const {skillId} = useParams<{skillId?: string}>();
    const location = useLocation();
    const navigate = useNavigate();

    const closeSkillDetail = useAiSkillsStore((state) => state.closeSkillDetail);
    const openSkillDetail = useAiSkillsStore((state) => state.openSkillDetail);
    const searchQuery = useAiSkillsStore((state) => state.searchQuery);
    const selectedSkillId = useAiSkillsStore((state) => state.selectedSkillId);
    const setSearchQuery = useAiSkillsStore((state) => state.setSearchQuery);
    const setSkillsView = useAiSkillsStore((state) => state.setSkillsView);
    const skillsHeaderInfo = useAiSkillsStore((state) => state.skillsHeaderInfo);
    const skillsView = useAiSkillsStore((state) => state.skillsView);

    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const setContext = useCopilotStore((state) => state.setContext);

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    const handleOpenCopilot = () => {
        setContext({mode: MODE.ASK, parameters: {}, source: Source.SKILLS});

        setCopilotPanelOpen(true);
    };

    const route = determineRoute(location.pathname, skillId);

    useEffect(() => {
        if (route === 'detail' && skillId && selectedSkillId !== skillId) {
            openSkillDetail(skillId, '');
        } else if (route !== 'detail' && skillsView === 'detail') {
            closeSkillDetail();
        }
    }, [route, skillId, selectedSkillId, skillsView, openSkillDetail, closeSkillDetail]);

    useEffect(() => {
        if (route === 'list') {
            if (skillsView === 'uploadForm' || skillsView === 'writeForm') {
                setSkillsView('list');
            }
        } else if (route !== 'detail' && skillsView !== route) {
            setSkillsView(route);
        }
    }, [route, skillsView, setSkillsView]);

    useEffect(() => {
        if (route === 'detail' && selectedSkillId && skillId !== selectedSkillId) {
            navigate(`${SKILLS_BASE_PATH}/${selectedSkillId}`);
        }
    }, [route, selectedSkillId, skillId, navigate]);

    const isDetailOrCreate = route !== 'list';
    const headerTitle = isDetailOrCreate ? (skillsHeaderInfo.title ?? 'Skill') : 'Skills';

    const showToolbar = route === 'list' && skillsView !== 'empty';

    const toolbarRight = showToolbar ? (
        <div className="flex items-center gap-2">
            <div className="relative">
                <SearchIcon className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-gray-400" />

                <Input
                    className="w-64 pl-9"
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder="Search skills..."
                    value={searchQuery}
                />
            </div>

            <AiSkillsCreateDropdown />

            {ff_4554 && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            className="[&_svg]:size-5"
                            icon={<SparklesIcon />}
                            onClick={handleOpenCopilot}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Open Copilot panel</TooltipContent>
                </Tooltip>
            )}
        </div>
    ) : undefined;

    return (
        <LayoutContainer
            header={<Header centerTitle position="main" right={toolbarRight} title={headerTitle} />}
            leftSidebarBody={<AiSidebarNav currentSection="skills" />}
            leftSidebarHeader={<Header position="sidebar" title="AI" />}
            leftSidebarWidth="64"
        >
            <div className="flex min-h-0 w-full flex-col px-4 3xl:mx-auto 3xl:w-4/5">
                <AiSkillsPanel />
            </div>
        </LayoutContainer>
    );
};

export default AiSkills;
