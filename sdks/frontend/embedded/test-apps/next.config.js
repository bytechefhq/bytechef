/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    transpilePackages: ['@bytechef/embedded'],
    experimental: {
        optimizePackageImports: ['@bytechef/embedded'],
    },
    turbopack: {},
};

export default nextConfig;
