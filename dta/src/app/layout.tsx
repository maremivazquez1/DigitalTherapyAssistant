import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Digital Therapy Assistabt",
  description: "Experience personalized mental health support - anytime, anywhere.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" data-theme="calming">
      <body>
        {children}
      </body>
    </html>
  );
}
