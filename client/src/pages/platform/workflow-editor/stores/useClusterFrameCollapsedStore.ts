import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ClusterFrameCollapsedStateI {
    collapsedByWorkflowId: Record<string, Record<string, true>>;
    setClusterFrameCollapsed: (workflowId: string, clusterRootId: string, collapsed: boolean) => void;
}

const useClusterFrameCollapsedStore = create<ClusterFrameCollapsedStateI>()(
    devtools(
        persist(
            (set) => ({
                collapsedByWorkflowId: {},

                setClusterFrameCollapsed: (workflowId: string, clusterRootId: string, collapsed: boolean) =>
                    set((state) => {
                        const collapsedByWorkflowId = {...state.collapsedByWorkflowId};

                        if (collapsed) {
                            collapsedByWorkflowId[workflowId] = {
                                ...collapsedByWorkflowId[workflowId],
                                [clusterRootId]: true,
                            };

                            return {collapsedByWorkflowId};
                        }

                        const collapsedRootIds = {...collapsedByWorkflowId[workflowId]};

                        delete collapsedRootIds[clusterRootId];

                        if (Object.keys(collapsedRootIds).length) {
                            collapsedByWorkflowId[workflowId] = collapsedRootIds;
                        } else {
                            delete collapsedByWorkflowId[workflowId];
                        }

                        return {collapsedByWorkflowId};
                    }),
            }),
            {
                name: 'bytechef.cluster-frame-collapsed',
            }
        )
    )
);

export default useClusterFrameCollapsedStore;
