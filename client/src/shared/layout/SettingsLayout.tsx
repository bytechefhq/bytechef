import {usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {resolvePlatformMode} from '@/shared/navigation/platformMode';
import {useMemo} from 'react';
import {useLocation} from 'react-router-dom';

import Settings from './Settings';
import {getSettingsNavItems} from './settingsNavItems';

const SettingsLayout = () => {
    const currentType = usePlatformTypeStore((state) => state.currentType);

    const {pathname} = useLocation();

    const platformMode = resolvePlatformMode(pathname, currentType);

    const sidebarNavItems = useMemo(() => getSettingsNavItems(platformMode), [platformMode]);

    return <Settings sidebarNavItems={sidebarNavItems} />;
};

export default SettingsLayout;
