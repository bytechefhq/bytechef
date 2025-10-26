import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import IntegrationListItem from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem';
import {Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';

import IntegrationWorkflowList from '../integration-workflow-list/IntegrationWorkflowList';

const IntegrationList = ({integrations, tags}: {integrations: Integration[]; tags: Tag[]}) => {
    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            {integrations.map((integration) => {
                const integrationTagIds = integration.tags?.map((tag) => tag.id);

                return (
                    <Collapsible className="group" key={integration.id}>
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
