import '@testing-library/jest-dom';
import {render, screen} from '../../utils/test-utils';
import Button from './Button';
import {vi} from 'vitest';

describe('Button', async () => {
    it('should render the button', () => {
        const handleClick = vi.fn();

        render(<Button label="New Integration" onClick={handleClick} />);

        expect(screen.getByText('New Integration')).toBeInTheDocument();
    });
});
