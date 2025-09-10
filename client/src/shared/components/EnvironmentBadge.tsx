import {Badge} from '@/components/ui/badge';
import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';

const EnvironmentBadge = ({
    environmentId,
    variant = 'outline',
}: {
    environmentId: number;
    variant?: 'outline' | 'secondary';
}) => {
    const environments = useEnvironmentStore((state) => state.environments);

    const environment = environments?.find((environment) => +environment!.id! === environmentId);

    if (!environment) {
        return <></>;
    }

    return <Badge variant={variant}>{environment.name}</Badge>;
};

export default EnvironmentBadge;
