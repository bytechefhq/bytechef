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
import useAiSkills from '@/pages/automation/ai/skills/hooks/useAiSkills';
import useAiSkillsTagFilterGroups from '@/pages/automation/ai/skills/hooks/useAiSkillsTagFilterGroups';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import {useAiSkillsStore} from '@/pages/automation/ai/skills/stores/useAiSkillsStore';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import FilterBadges from '@/shared/components/filters/FilterBadges';
import FilterMenu, {hasActiveFilters} from '@/shared/components/filters/FilterMenu';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {
    ArrowLeftIcon,
    CodeIcon,
    DownloadIcon,
    EyeIcon,
    MoreVerticalIcon,
    SaveIcon,
    SearchIcon,
    SparklesIcon,
    Trash2Icon,
} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

const AiSkills = () => {
    const searchQuery = useAiSkillsStore((state) => state.searchQuery);
    const setSearchQuery = useAiSkillsStore((state) => state.setSearchQuery);

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

    const {handleBack, headerTitle, isDetailView, showSearchAndCreate, showToolbar} = useAiSkills();

    const tagFilterGroups = useAiSkillsTagFilterGroups();

    const ff_4554 = useFeatureFlagsStore()('ff-4554');

    let toolbarRight: React.ReactNode = undefined;

    if (showToolbar) {
        toolbarRight = (
            <div className="flex items-center gap-2">
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

                {showSearchAndCreate && <FilterMenu groups={tagFilterGroups} title="Filter Skills" />}

                <CopilotButton source={Source.SKILLS} />

                {showSearchAndCreate && <AiSkillsCreateDropdown />}
            </div>
        );
    } else if (isDetailView && handlers) {
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

    // The detail view used to keep a skills-list sidebar for switching between skills. Inside Settings the
    // only sidebar on screen is the settings nav, so the way back to the list is an explicit control instead
    // — the CustomComponentDetail idiom, which solves the same problem one settings entry over.
    const headerTitleContent = isDetailView ? (
        <div className="flex items-center gap-2">
            <Button
                aria-label="Back to skills"
                icon={<ArrowLeftIcon className="size-5" />}
                onClick={handleBack}
                size="icon"
                variant="ghost"
            />

            <span>{headerTitle}</span>
        </div>
    ) : (
        headerTitle
    );

    return (
        <LayoutContainer
            header={
                <Header
                    description={isDetailView ? undefined : 'Reusable instructions any AI agent can load.'}
                    position="main"
                    right={toolbarRight}
                    title={headerTitleContent}
                />
            }
            leftSidebarOpen={false}
        >
            <div className="flex min-h-0 w-full flex-col p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                {showToolbar && showSearchAndCreate && hasActiveFilters(tagFilterGroups) && (
                    <div className="flex flex-wrap items-center gap-2 pt-0 pb-4">
                        <FilterBadges groups={tagFilterGroups} />
                    </div>
                )}

                <AiSkillsPanel />
            </div>
        </LayoutContainer>
    );
};

export default AiSkills;
