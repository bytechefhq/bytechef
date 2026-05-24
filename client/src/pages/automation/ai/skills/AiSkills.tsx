import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiSidebarNav from '@/pages/automation/ai/components/AiSidebarNav';
import AiSkillsPanel from '@/pages/automation/ai/skills/AiSkillsPanel';
import AiSkillsCreateDropdown from '@/pages/automation/ai/skills/components/AiSkillsCreateDropdown';
import AiSkillsLeftSidebar from '@/pages/automation/ai/skills/components/AiSkillsLeftSidebar';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {DownloadIcon, PencilIcon, Plus, SaveIcon, SearchIcon, SparklesIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

type AiSkillsRouteType = 'detail' | 'list' | 'uploadForm' | 'writeForm';

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

    const closeSkillDetail = useAiSkillsStore((state) => state.closeSkillDetail);
    const openSkillDetail = useAiSkillsStore((state) => state.openSkillDetail);
    const searchQuery = useAiSkillsStore((state) => state.searchQuery);
    const selectedSkillId = useAiSkillsStore((state) => state.selectedSkillId);
    const setSearchQuery = useAiSkillsStore((state) => state.setSearchQuery);
    const setSkillsView = useAiSkillsStore((state) => state.setSkillsView);
    const skillsHeaderInfo = useAiSkillsStore((state) => state.skillsHeaderInfo);
    const skillsView = useAiSkillsStore((state) => state.skillsView);

    const {canSave, handlers, isSaving} = useAiSkillDetailToolbarStore(
        useShallow((state) => ({
            canSave: state.canSave,
            handlers: state.handlers,
            isSaving: state.isSaving,
        }))
    );

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    const queryClient = useQueryClient();

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.SKILLS, () => {
            queryClient.invalidateQueries({queryKey: ['aiSkills']});
        });
    }, [queryClient]);

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

    const isDetailOrCreate = route !== 'list';
    const headerTitle = isDetailOrCreate ? (skillsHeaderInfo.title ?? 'Skill') : 'Skills';

    const showToolbar = route === 'list' && skillsView !== 'empty';

    let toolbarRight: React.ReactNode = undefined;

    if (showToolbar) {
        toolbarRight = (
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
            </div>
        );
    } else if (route === 'detail' && handlers) {
        toolbarRight = (
            <div className="flex items-center gap-1">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            icon={<PencilIcon className="size-4" />}
                            onClick={handlers.onEdit}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Edit skill</TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            disabled={!canSave || isSaving}
                            icon={<SaveIcon className="size-4" />}
                            onClick={handlers.onSave}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Save changes</TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            icon={<DownloadIcon className="size-4" />}
                            onClick={handlers.onDownload}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Download skill</TooltipContent>
                </Tooltip>

                {ff_4554 && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="[&_svg]:size-5"
                                icon={<SparklesIcon />}
                                onClick={handlers.onCopilot}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Open Copilot panel</TooltipContent>
                    </Tooltip>
                )}
            </div>
        );
    }

    const isDetailView = route === 'detail';

    // In the skill detail view, swap the AI section nav for a Skills list (search + create) sidebar so
    // the user can switch between skills without leaving the detail surface — same pattern as Data Tables.
    const leftSidebarBody = isDetailView ? <AiSkillsLeftSidebar currentId={skillId} /> : <AiSidebarNav currentSection="skills" />;

    const leftSidebarHeader = isDetailView ? (
        <Header
            position="sidebar"
            right={
                <AiSkillsCreateDropdown
                    trigger={
                        <Button
                            aria-label="Create skill"
                            icon={<Plus className="size-4" />}
                            size="icon"
                            variant="ghost"
                        />
                    }
                />
            }
            title="Skills"
        />
    ) : (
        <Header position="sidebar" title="AI" />
    );

    return (
        <LayoutContainer
            header={<Header centerTitle position="main" right={toolbarRight} title={headerTitle} />}
            leftSidebarBody={leftSidebarBody}
            leftSidebarHeader={leftSidebarHeader}
            leftSidebarWidth="64"
        >
            <div className="flex min-h-0 w-full flex-col px-4 3xl:mx-auto 3xl:w-4/5">
                <AiSkillsPanel />
            </div>
        </LayoutContainer>
    );
};

export default AiSkills;
