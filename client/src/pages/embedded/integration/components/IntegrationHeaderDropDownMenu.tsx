import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {IntegrationModel, IntegrationStatusModel} from '@/shared/middleware/embedded/configuration';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {CaretDownIcon} from '@radix-ui/react-icons';
import * as React from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const IntegrationHeaderDropDownMenu = ({integration}: {integration: IntegrationModel}) => {
    const {data: integrations} = useGetIntegrationsQuery();

    const navigate = useNavigate();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button className="flex cursor-pointer items-center space-x-2 hover:bg-gray-200" variant="ghost">
                    <h1>{integration?.componentName}</h1>

                    {integration && (
                        <Badge
                            className="flex space-x-1"
                            variant={integration.status === IntegrationStatusModel.Published ? 'success' : 'outline'}
                        >
                            <span>V{integration.integrationVersion}</span>

                            <span>
                                {integration.status === IntegrationStatusModel.Published ? `Published` : 'Draft'}
                            </span>
                        </Badge>
                    )}

                    <CaretDownIcon />
                </Button>
            </DropdownMenuTrigger>

            {integrations && (
                <DropdownMenuContent align="start" className="max-h-96 w-96 space-y-2 overflow-y-scroll">
                    {integrations.map((curIntegration) => (
                        <DropdownMenuItem
                            className="flex cursor-pointer items-center justify-between"
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
                                    curIntegration.id === integration.id && 'font-semibold'
                                )}
                            >
                                <div className="flex items-center gap-2">
                                    <span>{curIntegration.componentName}</span>

                                    {curIntegration.category && (
                                        <span className="text-xs uppercase text-gray-700">
                                            {curIntegration.category.name}
                                        </span>
                                    )}
                                </div>

                                <div className="mr-1 text-xs">
                                    {curIntegration.integrationWorkflowIds?.length === 1
                                        ? `${curIntegration.integrationWorkflowIds?.length} workflow`
                                        : `${curIntegration.integrationWorkflowIds?.length} workflows`}
                                </div>
                            </div>

                            <Badge className="flex space-x-1" variant="secondary">
                                <span>V{curIntegration.integrationVersion}</span>

                                <span>Draft</span>
                            </Badge>
                        </DropdownMenuItem>
                    ))}
                </DropdownMenuContent>
            )}
        </DropdownMenu>
    );
};

export default IntegrationHeaderDropDownMenu;
