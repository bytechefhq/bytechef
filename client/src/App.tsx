import {Toaster} from '@/components/ui/toaster';
import useGlobalErrorInterceptor from '@/config/useGlobalErrorInterceptor';
import {DesktopSidebar} from '@/layouts/DesktopSidebar';
import {MobileSidebar} from '@/layouts/MobileSidebar';
import {MobileTopNavigation} from '@/layouts/MobileTopNavigation';
import {ActivityIcon, FolderIcon, Layers3Icon, Link2Icon, LucideIcon} from 'lucide-react';
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
        name: 'Project Instances',
    },
    {href: '/automation/connections', icon: Link2Icon, name: 'Connections'},
    {
        href: '/automation/executions',
        icon: ActivityIcon,
        name: 'Workflow Execution History',
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

    useGlobalErrorInterceptor();

    useEffect(() => {
        document.title = titles[location.pathname] ?? 'ByteChef';
    }, [location]);

    return (
        <div className="flex h-full">
            <TooltipProvider>
                <MobileSidebar
                    mobileMenuOpen={mobileMenuOpen}
                    navigation={navigation}
                    setMobileMenuOpen={setMobileMenuOpen}
                    user={user}
                />

                <DesktopSidebar navigation={navigation} />

                <div className="flex min-w-0 flex-1 flex-col">
                    <MobileTopNavigation setMobileMenuOpen={setMobileMenuOpen} />

                    <Outlet />
                </div>
            </TooltipProvider>

            <Toaster />
        </div>
    );
}

export default App;
