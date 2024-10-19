import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Type} from '@/pages/embedded/integrations/Integrations';
import IntegrationsLeftSidebarNav from '@/pages/embedded/integrations/components/IntegrationsLeftSidebarNav';
import {useGetIntegrationCategoriesQuery} from '@/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/shared/queries/embedded/integrationTags.quries';
import {FilterIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const IntegrationsSidebarFilterPopover = () => {
    const [searchParams] = useSearchParams();

    const filterData = {
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const {data: categories} = useGetIntegrationCategoriesQuery();

    const {data: tags} = useGetIntegrationTagsQuery();

    return (
        <Popover>
            <PopoverTrigger asChild>
                <div>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button className="size-8 hover:bg-background/50" size="icon" variant="ghost">
                                <FilterIcon className="size-5" />
                            </Button>
                        </TooltipTrigger>

                        <TooltipContent side="right">Filter projects</TooltipContent>
                    </Tooltip>
                </div>
            </PopoverTrigger>

            <PopoverContent align="end" className="p-2">
                <IntegrationsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />
            </PopoverContent>
        </Popover>
    );
};

export default IntegrationsSidebarFilterPopover;
