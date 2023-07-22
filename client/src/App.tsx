import {DesktopSidebar} from '@/layouts/DesktopSidebar';
import {MobileSidebar} from '@/layouts/MobileSidebar';
import {MobileTopNavigation} from '@/layouts/MobileTopNavigation';
import {
    FolderIcon,
    LinkIcon,
    QueueListIcon,
    RectangleStackIcon,
} from '@heroicons/react/24/outline';
import {useEffect, useState} from 'react';
import {Outlet, useLocation} from 'react-router-dom';

const user = {
    email: 'emily.selman@example.com',
    imageUrl:
        'https://images.unsplash.com/photo-1502685104226-ee32379fefbe?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80',
    name: 'Emily Selman',
};

const navigation: {
    name: string;
    href: string;
    icon: React.ForwardRefExoticComponent<
        Omit<React.SVGProps<SVGSVGElement>, 'ref'>
    >;
}[] = [
    {
        href: '/automation/projects',
        icon: FolderIcon,
        name: 'Projects',
    },
    {
        href: '/automation/project-instances',
        icon: RectangleStackIcon,
        name: 'Project Instances',
    },
    {href: '/automation/connections', icon: LinkIcon, name: 'Connections'},
    {
        href: '/automation/executions',
        icon: QueueListIcon,
        name: 'Executions',
    },
];

const titles: {[key: string]: string} = {
    '/': 'Projects',
    '/automation/connections': 'Connections',
    '/automation/executions': 'Executions',
    '/automation/project-instances': 'Project Instances',
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
            <MobileSidebar
                user={user}
                navigation={navigation}
                mobileMenuOpen={mobileMenuOpen}
                setMobileMenuOpen={setMobileMenuOpen}
            />

            <DesktopSidebar navigation={navigation} />

            <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
                <MobileTopNavigation setMobileMenuOpen={setMobileMenuOpen} />

                <Outlet />
            </div>
        </div>
    );
}

export default App;
