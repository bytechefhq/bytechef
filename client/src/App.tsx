import {DesktopSidebar} from '@/layouts/DesktopSidebar';
import {MobileSidebar} from '@/layouts/MobileSidebar';
import {MobileTopNavigation} from '@/layouts/MobileTopNavigation';
import {
    FolderIcon,
    Layers3Icon,
    Link2Icon,
    ListEndIcon,
    LucideIcon,
} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Outlet, useLocation} from 'react-router-dom';

import {TooltipProvider} from './components/ui/tooltip';

const user = {
    email: 'emily.selman@example.com',
    imageUrl:
        'https://images.unsplash.com/photo-1502685104226-ee32379fefbe?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80',
    name: 'Emily Selman',
};

const navigation: {
    name: string;
    href: string;
    icon: LucideIcon;
}[] = [
    {
        href: '/automation/projects',
        icon: FolderIcon,
        name: 'Projects',
    },
    {
        href: '/automation/instances',
        icon: Layers3Icon,
        name: 'Instances',
    },
    {href: '/automation/connections', icon: Link2Icon, name: 'Connections'},
    {
        href: '/automation/executions',
        icon: ListEndIcon,
        name: 'Executions',
    },
];

const titles: {[key: string]: string} = {
    '/': 'Projects',
    '/automation/connections': 'Connections',
    '/automation/executions': 'Executions',
    '/automation/instances': 'Instances',
    '/automation/projects': 'Projects',
};

function App() {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    const location = useLocation();

    useEffect(() => {
        document.title = titles[location.pathname] ?? 'Bytechef';
    }, [location]);

    return (
        <div className="flex h-full">
            <TooltipProvider>
                <MobileSidebar
                    user={user}
                    navigation={navigation}
                    mobileMenuOpen={mobileMenuOpen}
                    setMobileMenuOpen={setMobileMenuOpen}
                />

                <DesktopSidebar navigation={navigation} />

                <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
                    <MobileTopNavigation
                        setMobileMenuOpen={setMobileMenuOpen}
                    />

                    <Outlet />
                </div>
            </TooltipProvider>
        </div>
    );
}

export default App;
