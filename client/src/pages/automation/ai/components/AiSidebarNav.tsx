import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';

const AI_BASE_PATH = '/automation/ai';

interface SectionDefinitionI {
    id: string;
    label: string;
}

const SECTIONS: SectionDefinitionI[] = [{id: 'skills', label: 'Skills'}];

const sectionPath = (id: string) => `${AI_BASE_PATH}/${id}`;

interface AiSidebarNavPropsI {
    currentSection: string;
}

const AiSidebarNav = ({currentSection}: AiSidebarNavPropsI) => (
    <LeftSidebarNav
        body={
            <>
                {SECTIONS.map((section) => (
                    <LeftSidebarNavItem
                        item={{current: currentSection === section.id, name: section.label}}
                        key={section.id}
                        toLink={sectionPath(section.id)}
                    />
                ))}
            </>
        }
    />
);

export default AiSidebarNav;
