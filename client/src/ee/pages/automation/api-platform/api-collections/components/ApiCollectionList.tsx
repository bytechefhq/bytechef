import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ApiCollection, Tag} from '@/ee/shared/middleware/automation/api-platform';

import ApiCollectionEndpointList from './ApiCollectionEndpointList';
import ApiCollectionListItem from './ApiCollectionListItem';

const ApiCollectionList = ({apiCollections, tags}: {apiCollections: ApiCollection[]; tags?: Tag[]}) => {
    return (
        <>
            {apiCollections.map((apiCollection) => {
                return (
                    <Collapsible className="group" key={apiCollection.id}>
                        <ApiCollectionListItem apiCollection={apiCollection} tags={tags} />

                        <CollapsibleContent>
                            <ApiCollectionEndpointList
                                apiCollectionEndpoints={apiCollection.endpoints}
                                apiCollectionId={apiCollection.id!}
                                collectionVersion={apiCollection.collectionVersion!}
                                projectDeploymentId={apiCollection.projectDeploymentId!}
                                projectId={apiCollection.projectId}
                                projectVersion={apiCollection.projectVersion}
                            />
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </>
    );
};

export default ApiCollectionList;
