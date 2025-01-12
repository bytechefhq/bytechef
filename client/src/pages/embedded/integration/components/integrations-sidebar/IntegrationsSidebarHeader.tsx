import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationsLeftSidebarStore from '@/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {Type} from '@/pages/embedded/integrations/Integrations';
import IntegrationsLeftSidebarNav from '@/pages/embedded/integrations/components/IntegrationsLeftSidebarNav';
import {useGetIntegrationCategoriesQuery} from '@/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/shared/queries/embedded/integrationTags.quries';
import {FilterIcon, PanelLeftIcon} from 'lucide-react';
import * as React from 'react';
import {useSearchParams} from 'react-router-dom';

const IntegrationsSidebarHeader = () => {
    const {leftSidebarOpen, setLeftSidebarOpen} = useIntegrationsLeftSidebarStore();

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
        <div className="flex items-center">
            <Popover>
                <PopoverTrigger asChild>
                    <div>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    className="size-8 hover:bg-background/50 [&_svg]:size-5"
                                    size="icon"
                                    variant="ghost"
                                >
                                    <FilterIcon />
                                </Button>
                            </TooltipTrigger>

                            <TooltipContent side="right">Filter integrations</TooltipContent>
                        </Tooltip>
                    </div>
                </PopoverTrigger>

                <PopoverContent align="end" className="p-2">
                    <IntegrationsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />
                </PopoverContent>
            </Popover>

            {leftSidebarOpen && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            className="hover:bg-background/70 [&_svg]:size-5"
                            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                            size="icon"
                            variant="ghost"
                        >
                            <PanelLeftIcon />
                        </Button>
                    </TooltipTrigger>

                    <TooltipContent>See integrations</TooltipContent>
                </Tooltip>
            )}
        </div>
    );
};

export default IntegrationsSidebarHeader;
