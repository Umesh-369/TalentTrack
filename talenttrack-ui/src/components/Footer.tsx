"use client";

import Link from "next/link";
import { Briefcase } from "lucide-react";

export default function Footer() {
  return (
    <footer className="border-t border-zinc-200 bg-zinc-50/50 py-12 px-6">
      <div className="mx-auto max-w-7xl flex flex-col md:flex-row items-center justify-between gap-6">
        {/* Logo */}
        <div className="flex items-center gap-2 font-bold text-zinc-900 tracking-wide">
          <Briefcase className="h-5 w-5 text-sky-600" />
          <span>Talent<span className="text-sky-600">Track</span></span>
        </div>

        {/* Links */}
        <div className="flex flex-wrap items-center justify-center gap-8 text-sm text-zinc-600">
          <Link href="#" className="hover:text-zinc-900 transition-colors">Privacy Policy</Link>
          <Link href="#" className="hover:text-zinc-900 transition-colors">Terms of Service</Link>
          <Link href="#" className="hover:text-zinc-900 transition-colors">Documentation</Link>
          <Link href="#" className="hover:text-zinc-900 transition-colors">Support</Link>
        </div>

        {/* Copy */}
        <p className="text-xs text-zinc-500">
          &copy; {new Date().getFullYear()} TalentTrack. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
