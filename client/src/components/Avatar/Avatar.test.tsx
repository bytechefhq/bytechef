import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import Avatar from './Avatar';

describe('<Avatar />', () => {
    const props = {
        alt: 'Avatar',
        src: 'https://gravatar.com/4405735f6f3129e0286d9d43e7b460d0',
    };

    it('should render the medium Avatar as default', () => {
        const {container} = render(<Avatar {...props} />);

        expect(screen.getByRole('img', {name: /Avatar/i})).toBeInTheDocument();

        expect(container.firstChild).toHaveClass(
            'mx-auto block rounded-full w-12 h-12'
        );

        expect(container.firstChild).toMatchSnapshot();
    });

    it('should render the small Avatar', () => {
        const {container} = render(<Avatar size="small" {...props} />);

        expect(screen.getByRole('img', {name: /Avatar/i})).toBeInTheDocument();

        expect(container.firstChild).toHaveClass(
            'mx-auto block rounded-full w-10 h-10'
        );

        expect(container.firstChild).toMatchSnapshot();
    });

    it('should render the medium Avatar', () => {
        const {container} = render(<Avatar size="medium" {...props} />);

        expect(screen.getByRole('img', {name: /Avatar/i})).toBeInTheDocument();

        expect(container.firstChild).toHaveClass(
            'mx-auto block rounded-full w-12 h-12'
        );
    });

    it('should render the large Avatar', () => {
        const {container} = render(<Avatar size="large" {...props} />);

        expect(screen.getByRole('img', {name: /Avatar/i})).toBeInTheDocument();

        expect(container.firstChild).toHaveClass(
            'mx-auto block rounded-full w-14 h-14'
        );

        expect(container.firstChild).toMatchSnapshot();
    });

    it('should render the empty Avatar', () => {
        const {container} = render(<Avatar />);

        expect(screen.getByTestId('empty-avatar')).toBeInTheDocument();

        expect(container.firstChild).toMatchSnapshot();
    });
});
