import {A2aProjectsByServerIdQuery, useA2aProjectsByServerIdQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export type A2aProjectItemType = NonNullable<NonNullable<A2aProjectsByServerIdQuery['a2aProjectsByServerId']>[number]>;

const useA2aProjectList = (a2aServerId: string) => {
    const {data: a2aProjectsData, isError, isLoading} = useA2aProjectsByServerIdQuery({a2aServerId});

    const a2aProjects = useMemo(
        () =>
            a2aProjectsData?.a2aProjectsByServerId?.filter(
                (a2aProject): a2aProject is A2aProjectItemType => a2aProject !== null
            ) || [],
        [a2aProjectsData]
    );

    return {a2aProjects, isError, isLoading};
};

export default useA2aProjectList;
