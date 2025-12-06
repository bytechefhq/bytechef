import Badge from '@/components/Badge/Badge';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

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

    const styleType = variant === 'outline' ? 'outline-outline' : 'secondary-filled';

    return <Badge label={environment.name} styleType={styleType} weight="semibold" />;
};

export default EnvironmentBadge;
