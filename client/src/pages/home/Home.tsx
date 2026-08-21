import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import ModeSelectionDialog from './components/ModeSelectionDialog';

const Home = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const currentType = usePlatformTypeStore((state) => state.currentType);

    const navigate = useNavigate();

    useEffect(() => {
        if (currentType !== undefined) {
            if (currentType === PlatformType.AUTOMATION) {
                navigate('/automation');
            } else if (currentType === PlatformType.EMBEDDED) {
                navigate('/embedded');
            }
        }

        if (currentType === undefined) {
            setIsDialogOpen(true);
        }
    }, [currentType, navigate]);

    if (!isDialogOpen) {
        return <></>;
    }

    return <ModeSelectionDialog handleDialogClose={() => setIsDialogOpen(false)} isDialogOpen={isDialogOpen} />;
};

export default Home;
