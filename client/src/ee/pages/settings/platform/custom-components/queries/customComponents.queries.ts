import {CustomComponent, CustomComponentApi} from '@/ee/shared/middleware/platform/custom-component';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const CustomComponentKeys = {
    customComponents: ['customComponents'] as const,
};

export const useGetCustomComponentsQuery = () =>
    useQuery<CustomComponent[], Error>({
        queryKey: CustomComponentKeys.customComponents,
        queryFn: () => new CustomComponentApi().getCustomComponents(),
    });
