import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {Navigate} from 'react-router-dom';

interface EmbeddedIndexRedirectProps {
    fallbackHref: string;
    tenantAdminHref: string;
}

const EmbeddedIndexRedirect = ({fallbackHref, tenantAdminHref}: EmbeddedIndexRedirectProps) => {
    const isTenantAdmin = useIsTenantAdmin();

    return <Navigate replace to={isTenantAdmin ? tenantAdminHref : fallbackHref} />;
};

export default EmbeddedIndexRedirect;
