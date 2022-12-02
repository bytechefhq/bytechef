import '@testing-library/jest-dom';
import {render, screen} from '../../utils/test-utils';
import {Button} from './Button';

describe('Button', async () => {
	it('should render the button', () => {
		render(<Button title="New Integration" />);
		expect(screen.getByText('New Integration')).toBeInTheDocument();
	});
});
