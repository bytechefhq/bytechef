import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useViewableEmbeddedPageHrefs} from '@/shared/hooks/useViewableEmbeddedPageHrefs';
import {Navigate} from 'react-router-dom';

interface EmbeddedIndexRedirectProps {
    fallbackHref: string;
    tenantAdminHref: string;
}

const EmbeddedIndexRedirect = ({fallbackHref, tenantAdminHref}: EmbeddedIndexRedirectProps) => {
    const isTenantAdmin = useIsTenantAdmin();
    const viewablePageHrefs = useViewableEmbeddedPageHrefs();

    return <Navigate replace to={isTenantAdmin ? tenantAdminHref : viewablePageHrefs[0] || fallbackHref} />;
};

export default EmbeddedIndexRedirect;
