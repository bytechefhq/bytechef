import ConnectedUserTriggerSubscriptionListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-trigger-subscription-list/ConnectedUserTriggerSubscriptionListItem';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useConnectedUserTriggerSubscriptionsQuery} from '@/shared/middleware/graphql';

interface ConnectedUserSheetPanelTriggerSubscriptionListProps {
    connectedUserId: number;
}

const ConnectedUserSheetPanelTriggerSubscriptionList = ({
    connectedUserId,
}: ConnectedUserSheetPanelTriggerSubscriptionListProps) => {
    const {data, isLoading} = useConnectedUserTriggerSubscriptionsQuery({connectedUserId: connectedUserId.toString()});

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    const triggerSubscriptions = data?.connectedUserTriggerSubscriptions ?? [];

    return (
        <div>
            <div className="flex items-center justify-between pb-2">
                <h3 className="text-sm font-semibold">Triggers</h3>
            </div>

            {triggerSubscriptions.length > 0 ? (
                <ul className="flex flex-col gap-y-1">
                    {triggerSubscriptions.map((triggerSubscription) => (
                        <ConnectedUserTriggerSubscriptionListItem
                            componentDefinition={componentDefinitions?.find(
                                (componentDefinition) => componentDefinition.name === triggerSubscription.componentName
                            )}
                            key={triggerSubscription.id}
                            triggerSubscription={triggerSubscription}
                        />
                    ))}
                </ul>
            ) : (
                <div className="py-4 text-sm">No trigger subscriptions. Create them with the Trigger API.</div>
            )}
        </div>
    );
};

export default ConnectedUserSheetPanelTriggerSubscriptionList;
