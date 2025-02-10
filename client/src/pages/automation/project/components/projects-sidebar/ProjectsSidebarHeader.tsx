import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Type} from '@/pages/automation/projects/Projects';
import ProjectsLeftSidebarNav from '@/pages/automation/projects/components/ProjectsLeftSidebarNav';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {FilterIcon, PanelLeftIcon} from 'lucide-react';
import * as React from 'react';

const ProjectsSidebarHeader = ({
    categories,
    filterData,
    onLeftSidebarOpenClick,
    tags,
}: {
    categories?: Category[];
    filterData: {id?: number; type: Type};
    onLeftSidebarOpenClick: () => void;
    tags?: Tag[];
}) => {
    return (
        <div className="flex items-center">
            <Popover>
                <PopoverTrigger asChild>
                    <div>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button className="size-8 hover:bg-muted [&_svg]:size-5" size="icon" variant="ghost">
                                    <FilterIcon />
                                </Button>
                            </TooltipTrigger>

                            <TooltipContent side="right">Filter projects</TooltipContent>
                        </Tooltip>
                    </div>
                </PopoverTrigger>

                <PopoverContent align="end" className="p-2">
                    <ProjectsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />
                </PopoverContent>
            </Popover>

            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        className="hover:bg-muted [&_svg]:size-5"
                        onClick={onLeftSidebarOpenClick}
                        size="icon"
                        variant="ghost"
                    >
                        <PanelLeftIcon />
                    </Button>
                </TooltipTrigger>

                <TooltipContent>See projects</TooltipContent>
            </Tooltip>
        </div>
    );
};

export default ProjectsSidebarHeader;
