import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectTitle from '@/pages/automation/project/components/project-header/components/ProjectTitle';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

it('should render the correct project title', () => {
    const mockProject = {
        lastStatus: ProjectStatus.Draft,
        lastVersion: 3,
        name: 'Test Project',
        workspaceId: 1,
    };

    render(
        <TooltipProvider>
            <ProjectTitle project={mockProject} />
        </TooltipProvider>
    );

    expect(screen.getByText(`Test Project`)).toBeInTheDocument();
    expect(screen.getByText(`V3`)).toBeInTheDocument();
    expect(screen.getByText(`DRAFT`)).toBeInTheDocument();
});
