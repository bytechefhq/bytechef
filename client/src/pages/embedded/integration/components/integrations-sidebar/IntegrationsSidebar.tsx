import {Badge} from '@/components/ui/badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import * as React from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const IntegrationsSidebar = ({integrationId}: {integrationId: number}) => {
    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const {data: integrations} = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    return (
        <div className="space-y-0.5 overflow-y-scroll px-2">
            {integrations &&
                integrations
                    .filter(
                        (curIntegration) =>
                            curIntegration.integrationWorkflowIds && curIntegration.integrationWorkflowIds.length > 0
                    )
                    .map((curIntegration) => (
                        <div
                            className={twMerge(
                                'py-3 px-2 flex cursor-pointer items-center justify-between hover:bg-background/50 rounded-lg text-sm',
                                curIntegration.id === integrationId && 'bg-background/50'
                            )}
                            key={curIntegration.id}
                            onClick={() =>
                                navigate(
                                    `/embedded/integrations/${curIntegration?.id}/integration-workflows/${curIntegration?.integrationWorkflowIds![0]}`
                                )
                            }
                        >
                            <div
                                className={twMerge(
                                    'flex flex-col gap-1',
                                    curIntegration.id === integrationId && 'font-semibold'
                                )}
                            >
                                <div className="flex">
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <div className="max-w-56 overflow-hidden truncate">
                                                {curIntegration.name}
                                            </div>
                                        </TooltipTrigger>

                                        <TooltipContent>{curIntegration.name}</TooltipContent>
                                    </Tooltip>
                                </div>

                                <div className="mr-1 text-xs">
                                    {curIntegration.integrationWorkflowIds?.length === 1
                                        ? `${curIntegration.integrationWorkflowIds?.length} workflow`
                                        : `${curIntegration.integrationWorkflowIds?.length} workflows`}
                                </div>
                            </div>

                            {curIntegration.lastPublishedDate && curIntegration.lastIntegrationVersion ? (
                                <Badge className="flex space-x-1" variant="success">
                                    <span>V{curIntegration.lastIntegrationVersion - 1}</span>

                                    <span>PUBLISHED</span>
                                </Badge>
                            ) : (
                                <Badge className="flex space-x-1" variant="outline">
                                    <span>V{curIntegration.lastIntegrationVersion}</span>

                                    <span>{curIntegration.lastStatus}</span>
                                </Badge>
                            )}
                        </div>
                    ))}
        </div>
    );
};

export default IntegrationsSidebar;
