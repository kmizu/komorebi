import type { NextConfig } from "next";
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./i18n/request.ts');

const nextConfig: NextConfig = {
  // No native modules needed (using @libsql/client instead of better-sqlite3)

  // Proxy API calls to the Scala backend
  async rewrites() {
    const backendPort = process.env.BACKEND_PORT ?? '8080';
    return [
      {
        source: '/api/:path*',
        destination: `http://localhost:${backendPort}/api/:path*`,
      },
    ];
  },
};

export default withNextIntl(nextConfig);
