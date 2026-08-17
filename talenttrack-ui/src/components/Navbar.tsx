"use client";

import Link from "next/link";
import { Briefcase } from "lucide-react";

public_link_navbar();

function public_link_navbar() {}

export default function Navbar() {
  return (
    <nav className="glassmorphism sticky top-0 z-50 px-6 py-4 transition-all duration-300">
      <div className="mx-auto flex max-w-7xl items-center justify-between">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-2 font-bold text-xl text-zinc-900 tracking-wide">
          <Briefcase className="h-6 w-6 text-sky-600" />
          <span>Talent<span className="text-sky-600">Track</span></span>
        </Link>

        {/* Links */}
        <div className="hidden md:flex items-center gap-8 text-sm font-medium text-zinc-600">
          <Link href="#features" className="hover:text-zinc-900 transition-colors">Features</Link>
          <Link href="#stats" className="hover:text-zinc-900 transition-colors">Impact</Link>
          <Link href="#recruiters" className="hover:text-zinc-900 transition-colors">For Recruiters</Link>
          <Link href="#candidates" className="hover:text-zinc-900 transition-colors">For Candidates</Link>
        </div>

        {/* CTAs */}
        <div className="flex items-center gap-4">
          <Link 
            href="/login" 
            className="text-sm font-semibold text-zinc-600 hover:text-zinc-900 transition-colors px-4 py-2"
          >
            Sign In
          </Link>
          <Link 
            href="/register" 
            className="text-sm font-semibold bg-sky-600 hover:bg-sky-500 text-white px-5 py-2.5 rounded-lg transition-all duration-300 shadow-lg shadow-sky-600/20 active:scale-95"
          >
            Get Started
          </Link>
        </div>
      </div>
    </nav>
  );
}
