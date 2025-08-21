import {Badge} from '@/components/ui/badge';
import {useGetEnvironmentsQuery} from '@/shared/queries/platform/environments.queries';

const EnvironmentBadge = ({
    environmentId,
    variant = 'outline',
}: {
    environmentId: number;
    variant?: 'outline' | 'secondary';
}) => {
    const {data: environments} = useGetEnvironmentsQuery();

    const environment = environments?.find((environment) => +environment!.id! === environmentId);

    if (!environment) {
        return <></>;
    }

    return <Badge variant={variant}>{environment.name}</Badge>;
};

export default EnvironmentBadge;
