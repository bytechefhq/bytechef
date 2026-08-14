import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiSkillsPanel from '@/pages/automation/ai/skills/AiSkillsPanel';
import AiSkillsCreateDropdown from '@/pages/automation/ai/skills/components/AiSkillsCreateDropdown';
import AiSkillsLeftSidebar from '@/pages/automation/ai/skills/components/AiSkillsLeftSidebar';
import AiSkillsTagsNav from '@/pages/automation/ai/skills/components/AiSkillsTagsNav';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import invalidateSkillQueries from '@/pages/automation/ai/skills/utils/invalidateSkillQueries';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
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
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

type AiSkillsRouteType = 'detail' | 'list';

const determineRoute = (skillId: string | undefined): AiSkillsRouteType => {
    return skillId ? 'detail' : 'list';
};

const AiSkills = () => {
    const {skillId} = useParams<{skillId?: string}>();

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

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    useEffect(() => {
        return registerPostTurn(Source.SKILLS, () => {
            invalidateSkillQueries(queryClient);
        });
    }, [queryClient, registerPostTurn]);

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

    const route = determineRoute(skillId);

    useEffect(() => {
        if (route === 'detail' && skillId && selectedSkillId !== skillId) {
            openSkillDetail(skillId, '');
        } else if (route === 'list' && skillsView === 'detail') {
            closeSkillDetail();
        }
    }, [closeSkillDetail, openSkillDetail, route, selectedSkillId, skillId, skillsView]);

    const headerTitle = route === 'detail' ? (skillsHeaderInfo.title ?? 'Skill') : 'Skills';

    const showToolbar = route === 'list';
    const showSearchAndCreate = skillsView !== 'empty';

    let toolbarRight: React.ReactNode = undefined;

    if (showToolbar) {
        toolbarRight = (
            <div className="flex items-center gap-1">
                {showSearchAndCreate && (
                    <div className="relative">
                        <SearchIcon className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-content-neutral-tertiary" />

                        <Input
                            className="w-64 pl-9"
                            onChange={(event) => setSearchQuery(event.target.value)}
                            placeholder="Search skills..."
                            value={searchQuery}
                        />
                    </div>
                )}

                <CopilotButton source={Source.SKILLS} />

                {showSearchAndCreate && <AiSkillsCreateDropdown />}
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

                        <DropdownMenuItem onClick={handlers.onDelete} variant="destructive">
                            <Trash2Icon className="mr-2 size-4" /> Delete Skill
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        );
    }

    const isDetailView = route === 'detail';

    // The detail view keeps the skills-list sidebar for switching between skills — navigation only,
    // no Tags: a tag filter there has nothing to filter. The list view renders the Tags filter alone
    // (the data-tables idiom).
    const showSkillsSidebar = isDetailView;

    const leftSidebarBody = showSkillsSidebar ? <AiSkillsLeftSidebar currentId={skillId} /> : <AiSkillsTagsNav />;

    const leftSidebarHeader = showSkillsSidebar ? (
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
        <Header position="sidebar" title="Skills" />
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
