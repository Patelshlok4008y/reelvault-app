import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "REELVAULT | Your personal screen library",
  description: "A local, cinematic movie and series library.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
