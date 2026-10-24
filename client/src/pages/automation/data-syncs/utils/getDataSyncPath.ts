interface DataSyncPathDataSyncI {
    id: string;
    projectId: string;
}

const getDataSyncPath = ({id, projectId}: DataSyncPathDataSyncI): string =>
    `/automation/projects/${projectId}/data-syncs/${id}`;

export default getDataSyncPath;
