import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import EmbeddedWorkflowExecutionIntegration from '../EmbeddedWorkflowExecutionIntegration';

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{icon: 'gmail.svg', name: 'googleMail', title: 'Gmail'}]}),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="" data-testid="component-icon" src={src} />,
}));

describe('EmbeddedWorkflowExecutionIntegration', () => {
    it('shows the component title and icon of a known component', () => {
        render(<EmbeddedWorkflowExecutionIntegration componentName="googleMail" />);

        expect(screen.getByText('Gmail')).toBeInTheDocument();
        expect(screen.getByTestId('component-icon')).toHaveAttribute('src', 'gmail.svg');
    });

    it('falls back to the component name when the definition is unknown', () => {
        render(<EmbeddedWorkflowExecutionIntegration componentName="hubspot" />);

        expect(screen.getByText('hubspot')).toBeInTheDocument();
        expect(screen.queryByTestId('component-icon')).not.toBeInTheDocument();
    });

    it('renders nothing without a component name', () => {
        const {container} = render(<EmbeddedWorkflowExecutionIntegration />);

        expect(container).toBeEmptyDOMElement();
    });
});
