import Button from '@/components/Button/Button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {Type} from '@/ee/pages/embedded/integrations/Integrations';
import IntegrationsLeftSidebarNav from '@/ee/pages/embedded/integrations/components/IntegrationsLeftSidebarNav';
import {useGetIntegrationCategoriesQuery} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {FilterIcon, PanelLeftIcon} from 'lucide-react';
import * as React from 'react';
import {useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const IntegrationsSidebarHeader = () => {
    const {leftSidebarOpen, setLeftSidebarOpen} = useIntegrationsLeftSidebarStore(
        useShallow((state) => ({
            leftSidebarOpen: state.leftSidebarOpen,
            setLeftSidebarOpen: state.setLeftSidebarOpen,
        }))
    );

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
                                    className="size-8 hover:bg-muted [&_svg]:size-5"
                                    icon={<FilterIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
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
                            className="hover:bg-muted [&_svg]:size-5"
                            icon={<PanelLeftIcon />}
                            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>See integrations</TooltipContent>
                </Tooltip>
            )}
        </div>
    );
};

export default IntegrationsSidebarHeader;
