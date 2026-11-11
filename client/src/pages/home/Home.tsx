import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import ModeSelectionDialog from './components/ModeSelectionDialog';

const Home = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const edition = useApplicationInfoStore((state) => state.application?.edition);
    const currentType = usePlatformTypeStore((state) => state.currentType);

    const navigate = useNavigate();

    useEffect(() => {
        if (currentType === PlatformType.AUTOMATION) {
            void navigate('/automation');
        } else if (currentType === PlatformType.EMBEDDED) {
            void navigate('/embedded');
        } else if (edition === EditionType.EE) {
            setIsDialogOpen(true);
        } else if (edition !== undefined) {
            void navigate('/automation');
        }
    }, [currentType, edition, navigate]);

    if (!isDialogOpen) {
        return <></>;
    }

    return <ModeSelectionDialog handleDialogClose={() => setIsDialogOpen(false)} isDialogOpen={isDialogOpen} />;
};

export default Home;
