import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Type} from '@/pages/automation/projects/Projects';
import ProjectsLeftSidebarNav from '@/pages/automation/projects/components/ProjectsLeftSidebarNav';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {FilterIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const ProjectsSidebarFilterPopover = () => {
    const [searchParams] = useSearchParams();

    const filterData = {
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const {data: categories} = useGetProjectCategoriesQuery();

    const {data: tags} = useGetProjectTagsQuery();

    return (
        <Popover>
            <PopoverTrigger asChild>
                <div>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button className="size-8" size="icon" variant="ghost">
                                <FilterIcon className="size-5" />
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
    );
};

export default ProjectsSidebarFilterPopover;
