import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";
import Link from "next/link";

const geist = Geist({
  variable: "--font-geist",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Mindfulness Supervisor",
  description: "A supervision-first mindfulness app — not a teacher, a safety monitor.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${geist.variable} font-sans antialiased bg-white text-stone-800`}>
        {/* Minimal nav */}
        <nav className="border-b border-stone-100">
          <div className="max-w-2xl mx-auto px-6 py-4 flex items-center justify-between">
            <Link href="/" className="text-sm text-stone-600 hover:text-stone-900 transition-colors">
              Mindfulness Supervisor
            </Link>
            <div className="flex gap-6">
              <Link href="/session" className="text-sm text-stone-500 hover:text-stone-700 transition-colors">
                Session
              </Link>
              <Link href="/history" className="text-sm text-stone-500 hover:text-stone-700 transition-colors">
                History
              </Link>
            </div>
          </div>
        </nav>

        {/* Page content */}
        <main className="max-w-2xl mx-auto px-6 py-12">
          {children}
        </main>
      </body>
    </html>
  );
}
