"use client";

import { motion } from "framer-motion";
import Link from "next/link";
import { ArrowRight, ChevronRight, Play } from "lucide-react";

export default function Hero() {
  return (
    <section className="relative flex flex-col items-center justify-center px-6 pt-24 pb-16 overflow-hidden">
      {/* Background Glowing Gradients */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-sky-500/15 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute top-1/3 left-1/3 w-[300px] h-[300px] bg-cyan-500/15 rounded-full blur-[100px] pointer-events-none" />

      {/* Tagline Badge */}
      <motion.div 
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-sky-50 border border-sky-100 text-xs font-semibold text-sky-700 mb-8"
      >
        <span>⚡ Experience Next-Gen Hiring</span>
        <ChevronRight className="h-3 w-3" />
      </motion.div>

      {/* Headings */}
      <div className="text-center max-w-4xl">
        <motion.h1 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.1 }}
          className="text-4xl sm:text-6xl font-extrabold tracking-tight text-zinc-900 leading-[1.1] mb-6"
        >
          Recruiting, Reimagined for <br />
          <span className="bg-gradient-to-r from-sky-600 via-cyan-600 to-sky-400 bg-clip-text text-transparent">
            Modern Hiring Teams.
          </span>
        </motion.h1>

        <motion.p 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.3 }}
          className="text-lg sm:text-xl text-zinc-650 max-w-2xl mx-auto mb-10 leading-relaxed"
        >
          TalentTrack is a multi-tenant ATS platform with absolute data isolation for recruiters and snapping progress trackers for candidates. Speed up your hiring process today.
        </motion.p>
      </div>

      {/* CTA Buttons */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, delay: 0.4 }}
        className="flex flex-col sm:flex-row items-center gap-4 mb-16"
      >
        <Link 
          href="/register?role=recruiter" 
          className="w-full sm:w-auto flex items-center justify-center gap-2 px-8 py-4 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-semibold transition-all duration-300 shadow-lg shadow-sky-600/30 active:scale-95 group"
        >
          <span>For Employers</span>
          <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
        </Link>
        <Link 
          href="/jobs" 
          className="w-full sm:w-auto flex items-center justify-center gap-2 px-8 py-4 rounded-xl bg-white border border-zinc-200 hover:border-zinc-300 text-zinc-700 hover:text-zinc-950 font-semibold transition-all duration-300 active:scale-95"
        >
          <Play className="h-4 w-4 fill-zinc-650 text-zinc-650" />
          <span>Browse Jobs</span>
        </Link>
      </motion.div>

      {/* Visual Product Mockup */}
      <motion.div 
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 1, delay: 0.5 }}
        className="w-full max-w-5xl mx-auto rounded-2xl border border-zinc-200 bg-white/70 p-4 shadow-2xl relative backdrop-blur-md"
      >
        {/* Browser Frame Dots */}
        <div className="flex items-center gap-2 mb-3 pb-3 border-b border-zinc-200/80">
          <div className="w-3 h-3 rounded-full bg-rose-500/80" />
          <div className="w-3 h-3 rounded-full bg-amber-500/80" />
          <div className="w-3 h-3 rounded-full bg-emerald-500/80" />
        </div>

        {/* Mock Kanban Workspace */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 min-h-[300px] text-left">
          {/* Column APPLIED */}
          <div className="rounded-xl border border-zinc-200 bg-zinc-50/50 p-4">
            <div className="flex items-center justify-between mb-4">
              <span className="font-semibold text-zinc-700">Applied (3)</span>
              <span className="w-5 h-5 flex items-center justify-center rounded-full bg-zinc-200 text-zinc-650 text-xs">3</span>
            </div>
            <div className="space-y-3">
              <div className="p-3.5 rounded-lg border border-zinc-200 bg-white shadow-sm">
                <p className="font-semibold text-zinc-900 text-sm">Johnathan Doe</p>
                <p className="text-zinc-500 text-xs">Senior Fullstack Engineer</p>
              </div>
              <div className="p-3.5 rounded-lg border border-zinc-200 bg-white shadow-sm">
                <p className="font-semibold text-zinc-900 text-sm">Emily Watson</p>
                <p className="text-zinc-500 text-xs">UI/UX Product Designer</p>
              </div>
            </div>
          </div>

          {/* Column SHORTLISTED */}
          <div className="rounded-xl border border-zinc-200 bg-zinc-50/50 p-4">
            <div className="flex items-center justify-between mb-4">
              <span className="font-semibold text-zinc-700">Shortlisted (1)</span>
              <span className="w-5 h-5 flex items-center justify-center rounded-full bg-sky-100 text-sky-700 text-xs">1</span>
            </div>
            <div className="space-y-3">
              <div className="p-3.5 rounded-lg border border-sky-100 bg-sky-50 shadow-sm">
                <p className="font-semibold text-zinc-900 text-sm">Marcus Aurelius</p>
                <p className="text-zinc-500 text-xs">Lead Java Architect</p>
              </div>
            </div>
          </div>

          {/* Column INTERVIEW */}
          <div className="rounded-xl border border-zinc-200 bg-zinc-50/50 p-4">
            <div className="flex items-center justify-between mb-4">
              <span className="font-semibold text-zinc-700">Interviewing (2)</span>
              <span className="w-5 h-5 flex items-center justify-center rounded-full bg-cyan-100 text-cyan-700 text-xs font-semibold">2</span>
            </div>
            <div className="space-y-3">
              <div className="p-3.5 rounded-lg border border-zinc-200 bg-white shadow-sm">
                <p className="font-semibold text-zinc-900 text-sm">Sarah Connor</p>
                <p className="text-zinc-500 text-xs">Security Compliance Lead</p>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </section>
  );
}
