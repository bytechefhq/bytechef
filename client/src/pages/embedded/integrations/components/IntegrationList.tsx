import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {IntegrationModel, TagModel} from '@/middleware/embedded/configuration';
import IntegrationListItem from 'pages/embedded/integrations/components/IntegrationListItem';

import IntegrationWorkflowList from './IntegrationWorkflowList';

const IntegrationList = ({integrations, tags}: {integrations: IntegrationModel[]; tags: TagModel[]}) => {
    return (
        <div className="w-full divide-y divide-gray-100 px-2 3xl:mx-auto 3xl:w-4/5">
            {integrations.map((integration) => {
                const integrationTagIds = integration.tags?.map((tag) => tag.id);

                return (
                    <Collapsible key={integration.id}>
                        <IntegrationListItem
                            integration={integration}
                            key={integration.id}
                            remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                        />

                        <CollapsibleContent>
                            <IntegrationWorkflowList integration={integration} />
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};
export default IntegrationList;
