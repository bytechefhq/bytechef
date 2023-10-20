import {useQuery} from '@tanstack/react-query';
import {ProjectInstanceModel, ProjectInstancesApi} from 'middleware/project';

export const InstanceKeys = {
    instance: (id: number) => ['instance', id],
    instances: ['instances'] as const,
};

export const useGetInstancesQuery = () =>
    useQuery<ProjectInstanceModel[], Error>(InstanceKeys.instances, () =>
        new ProjectInstancesApi().getProjectInstances()
    );
