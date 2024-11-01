import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import React from 'react';
import {useNavigate} from 'react-router-dom';

import reactLogo from '../../../assets/logo.svg';

const DesktopSidebarTopMenu = () => {
    const {setCurrentType} = useModeTypeStore();

    const navigate = useNavigate();

    const ff_520 = useFeatureFlagsStore()('ff-520');

    const handleClick = (modeType: ModeType) => {
        setCurrentType(modeType);

        if (modeType === ModeType.AUTOMATION) {
            navigate('/automation');
        } else {
            navigate('/embedded');
        }
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <img alt="ByteChef" className="h-8 w-auto cursor-pointer" src={reactLogo} />
            </DropdownMenuTrigger>

            {ff_520 && (
                <DropdownMenuContent align="start" className="w-64 space-y-2 p-2" sideOffset={-35}>
                    <div className="flex items-center space-x-2 px-1.5 py-1 text-base font-semibold">
                        <img alt="ByteChef" className="h-5 w-auto" src={reactLogo} />

                        <span>ByteChef</span>
                    </div>

                    <DropdownMenuItem>
                        <button className="flex flex-col items-start" onClick={() => handleClick(ModeType.EMBEDDED)}>
                            <div className="font-semibold">Embedded</div>

                            <div className="text-muted-foreground">Build integrations for your product</div>
                        </button>
                    </DropdownMenuItem>

                    <DropdownMenuItem>
                        <button className="flex flex-col items-start" onClick={() => handleClick(ModeType.AUTOMATION)}>
                            <div className="font-semibold">Automation</div>

                            <div className="text-sm text-muted-foreground">Automate your daily work</div>
                        </button>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            )}
        </DropdownMenu>
    );
};

export default DesktopSidebarTopMenu;
