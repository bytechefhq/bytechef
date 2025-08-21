import {Environment, EnvironmentApi} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export const EnvironmentKeys = ['rqEnvironments'];

export const useGetEnvironmentsQuery = () =>
    useQuery<Environment[], Error>({
        queryFn: () => new EnvironmentApi().getEnvironments(),
        queryKey: EnvironmentKeys,
    });
