import '@testing-library/jest-dom';
import {MixIcon} from '@radix-ui/react-icons';
import {vi} from 'vitest';

import {render, screen} from '../../utils/test-utils';
import Button from './Button';

describe('Button', async () => {
    it('should render the button', () => {
        render(<Button label="New Project" />);

        expect(screen.getByText('New Project')).toBeInTheDocument();
    });

    it('should be interactive', () => {
        const handleClick = vi.fn();

        render(<Button id="btn" label="New Project" onClick={handleClick} />);

        const button = document.getElementById('btn');

        button!.click();

        expect(handleClick).toHaveBeenCalled();
    });

    it('supports different sizes and displayTypes', () => {
        render(
            <div id="container">
                <Button displayType="secondary" label="Secondary" />

                <Button displayType="danger" label="Danger" />

                <Button displayType="unstyled" label="Unstyled" />

                <Button displayType="icon" label="Icon" />

                <Button size="small" label="Small" />

                <Button size="large" label="Large" />
            </div>
        );

        const secondaryBtn = document.querySelector('.btn-secondary');
        const dangerBtn = document.querySelector('.btn-danger');
        const unstyledBtn = document.querySelector('.btn-unstyled');
        const iconBtn = document.querySelector('.btn-icon');
        const smallBtn = document.querySelector('.btn-small');
        const largeBtn = document.querySelector('.btn-icon');

        expect(secondaryBtn).toBeTruthy();
        expect(dangerBtn).toBeTruthy();
        expect(unstyledBtn).toBeTruthy();
        expect(iconBtn).toBeTruthy();
        expect(smallBtn).toBeTruthy();
        expect(largeBtn).toBeTruthy();
    });

    it('supports an icon', () => {
        render(
            <Button
                displayType="icon"
                icon={<MixIcon id="icon" name="icon" />}
                iconPosition="right"
                id="iconBtn"
                label="Secondary"
                size="small"
            />
        );

        const icon = document.getElementById('icon');

        expect(icon).toBeTruthy();
    });
});
