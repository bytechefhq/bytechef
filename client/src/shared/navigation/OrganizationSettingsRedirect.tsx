import {ORGANIZATION_SETTINGS_PATH, getOrganizationSettingsRedirectPath} from '@/shared/navigation/settingsPaths';
import {Navigate, useLocation} from 'react-router-dom';

const OrganizationSettingsRedirect = () => {
    const {hash, pathname, search} = useLocation();

    return (
        <Navigate
            replace
            to={getOrganizationSettingsRedirectPath(pathname, search, hash) ?? ORGANIZATION_SETTINGS_PATH}
        />
    );
};

export default OrganizationSettingsRedirect;
