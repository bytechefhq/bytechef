import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {useEffect} from 'react';

const PROJECT_PAGE_PATHNAME_PATTERN = /\/automation\/projects\/\d+\/(agents|data-syncs|project-workflows)\//;

export const useCloseProjectsLeftSidebarOnLeave = (pathname: string) => {
    const setProjectLeftSidebarOpen = useProjectsLeftSidebarStore((state) => state.setProjectLeftSidebarOpen);

    const onProjectPage = PROJECT_PAGE_PATHNAME_PATTERN.test(pathname);

    useEffect(() => {
        if (!onProjectPage) {
            setProjectLeftSidebarOpen(false);
        }
    }, [onProjectPage, setProjectLeftSidebarOpen]);
};
