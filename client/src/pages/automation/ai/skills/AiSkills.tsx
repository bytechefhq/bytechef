import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiSidebarNav from '@/pages/automation/ai/components/AiSidebarNav';
import AiSkillsPanel from '@/pages/automation/ai/skills/AiSkillsPanel';
import AiSkillsCreateDropdown from '@/pages/automation/ai/skills/components/AiSkillsCreateDropdown';
import AiSkillsLeftSidebar from '@/pages/automation/ai/skills/components/AiSkillsLeftSidebar';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    CodeIcon,
    DownloadIcon,
    EyeIcon,
    MoreVerticalIcon,
    Plus,
    SaveIcon,
    SearchIcon,
    SparklesIcon,
    Trash2Icon,
} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

type AiSkillsRouteType = 'createWithAi' | 'detail' | 'list';

const determineRoute = (skillId: string | undefined, pathname: string): AiSkillsRouteType => {
    if (pathname.endsWith('/create/ai')) {
        return 'createWithAi';
    }

    return skillId ? 'detail' : 'list';
};

const AiSkills = () => {
    const {skillId} = useParams<{skillId?: string}>();
    const location = useLocation();

    const closeSkillDetail = useAiSkillsStore((state) => state.closeSkillDetail);
    const openSkillDetail = useAiSkillsStore((state) => state.openSkillDetail);
    const searchQuery = useAiSkillsStore((state) => state.searchQuery);
    const selectedSkillId = useAiSkillsStore((state) => state.selectedSkillId);
    const setSearchQuery = useAiSkillsStore((state) => state.setSearchQuery);
    const skillsHeaderInfo = useAiSkillsStore((state) => state.skillsHeaderInfo);
    const skillsView = useAiSkillsStore((state) => state.skillsView);

    const {canSave, canToggleView, handlers, isSaving, viewMode} = useAiSkillDetailToolbarStore(
        useShallow((state) => ({
            canSave: state.canSave,
            canToggleView: state.canToggleView,
            handlers: state.handlers,
            isSaving: state.isSaving,
            viewMode: state.viewMode,
        }))
    );

    const setViewMode = useAiSkillDetailToolbarStore((state) => state.setViewMode);

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    const queryClient = useQueryClient();

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.SKILLS, () => {
            queryClient.invalidateQueries({queryKey: ['aiSkills']});
        });
    }, [queryClient]);

    useEffect(() => {
        return useCopilotStateContributorRegistry.getState().register(() => {
            const {selectedSkillId: activeSkillId, skillsHeaderInfo: activeHeaderInfo} = useAiSkillsStore.getState();

            if (activeSkillId == null) {
                return {};
            }

            return {
                currentSelectedSkillId: activeSkillId,
                currentSelectedSkillName: activeHeaderInfo.title,
            };
        });
    }, []);

    const route = determineRoute(skillId, location.pathname);

    const setSkillsView = useAiSkillsStore((state) => state.setSkillsView);

    useEffect(() => {
        if (route === 'detail' && skillId && selectedSkillId !== skillId) {
            openSkillDetail(skillId, '');
        } else if (route === 'createWithAi' && skillsView !== 'createWithAi') {
            setSkillsView('createWithAi');
        } else if (route === 'list' && (skillsView === 'detail' || skillsView === 'createWithAi')) {
            closeSkillDetail();
        }
    }, [
        closeSkillDetail,
        openSkillDetail,
        route,
        selectedSkillId,
        setSkillsView,
        skillId,
        skillsView,
    ]);

    const headerTitle =
        route === 'detail'
            ? (skillsHeaderInfo.title ?? 'Skill')
            : route === 'createWithAi'
              ? 'Create with AI'
              : 'Skills';

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
        const inSourceMode = viewMode === 'source';

        toolbarRight = (
            <div className="flex items-center gap-1">
                {canToggleView && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label={inSourceMode ? 'Show preview' : 'Show source'}
                                icon={inSourceMode ? <EyeIcon className="size-4" /> : <CodeIcon className="size-4" />}
                                onClick={() => setViewMode(inSourceMode ? 'preview' : 'source')}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>{inSourceMode ? 'Show preview' : 'Show source'}</TooltipContent>
                    </Tooltip>
                )}

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

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="More actions"
                            icon={<MoreVerticalIcon className="size-4" />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={handlers.onDownload}>
                            <DownloadIcon className="mr-2 size-4" /> Download Skill
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="text-content-destructive focus:text-content-destructive-primary"
                            onClick={handlers.onDelete}
                        >
                            <Trash2Icon className="mr-2 size-4" /> Delete Skill
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        );
    }

    const isDetailView = route === 'detail';

    const leftSidebarBody = isDetailView ? (
        <AiSkillsLeftSidebar currentId={skillId} />
    ) : (
        <AiSidebarNav currentSection="skills" />
    );

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
