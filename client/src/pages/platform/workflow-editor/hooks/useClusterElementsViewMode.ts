import useClusterElementsViewModeStore, {ClusterElementsViewModeType} from '../stores/useClusterElementsViewModeStore';

export default function useClusterElementsViewMode(): ClusterElementsViewModeType {
    return useClusterElementsViewModeStore((state) => state.clusterElementsViewMode);
}

export function getClusterElementsViewMode(): ClusterElementsViewModeType {
    return useClusterElementsViewModeStore.getState().clusterElementsViewMode;
}
