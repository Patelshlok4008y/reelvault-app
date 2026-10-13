const backend = process.env.REELVAULT_BACKEND_URL ?? "http://127.0.0.1:8081";

/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
