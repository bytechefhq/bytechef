/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    transpilePackages: ['@bytechef/automation-chat'],
    experimental: {
        optimizePackageImports: ['@bytechef/automation-chat'],
    },
    turbopack: {},
};

export default nextConfig;
