import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export type ClusterElementsViewModeType = 'box' | 'dialog';

interface ClusterElementsViewModeStateI {
    clusterElementsViewMode: ClusterElementsViewModeType;
    setClusterElementsViewMode: (clusterElementsViewMode: ClusterElementsViewModeType) => void;
}

const useClusterElementsViewModeStore = create<ClusterElementsViewModeStateI>()(
    devtools(
        persist(
            (set) => ({
                clusterElementsViewMode: 'dialog' as ClusterElementsViewModeType,

                setClusterElementsViewMode: (clusterElementsViewMode: ClusterElementsViewModeType) =>
                    set({clusterElementsViewMode}),
            }),
            {
                name: 'bytechef.cluster-elements-view-mode',
            }
        )
    )
);

export default useClusterElementsViewModeStore;
