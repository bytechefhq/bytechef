import { createMDX } from 'fumadocs-mdx/next';

const withMDX = createMDX();

/** @type {import('next').NextConfig} */
const config = {
  reactStrictMode: true,

  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'static.scarf.sh',
        pathname: '/**',
      },
    ],
  },

  async redirects() {
    return [
      {
        source: '/',
        destination: '/platform',
        permanent: true,
      },
    ];
  },
};

export default withMDX(config);
