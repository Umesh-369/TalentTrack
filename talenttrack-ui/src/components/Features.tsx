"use client";

import { motion } from "framer-motion";
import { Shield, GitPullRequest, Layers, Clock, UserCheck, Eye } from "lucide-react";

export default function Features() {
  const recruiterFeatures = [
    {
      icon: <Layers className="h-6 w-6 text-sky-600" />,
      title: "Decoupled Tenant Isolation",
      desc: "Robust physical and repository-level boundaries scope all actions strictly by company. Zero cross-leakage.",
    },
    {
      icon: <GitPullRequest className="h-6 w-6 text-cyan-600" />,
      title: "Interactive Kanban Pipeline",
      desc: "Drag-and-drop state-machine validation checks logic at the service layer prior to database transition writes.",
    },
    {
      icon: <Clock className="h-6 w-6 text-sky-500" />,
      title: "Daily Expiry Scheduler",
      desc: "Automated cron workers identify expired postings, mark them closed, and capture immutable audit trails.",
    },
  ];

  const candidateFeatures = [
    {
      icon: <Eye className="h-6 w-6 text-emerald-600" />,
      title: "Live Snap-Scroll Tracking",
      desc: "Candidates track status transitions in real-time. Full transparency on shortlists, interviews, and offers.",
    },
    {
      icon: <UserCheck className="h-6 w-6 text-teal-600" />,
      title: "Social OAuth Authentication",
      desc: "Instant candidate profile registration using Google ID token verification and zero-friction logins.",
    },
    {
      icon: <Shield className="h-6 w-6 text-cyan-600" />,
      title: "Strict Candidate Privacy",
      desc: "A candidate can only view their own submissions, resumes, and scheduled interview records. Complete confidentiality.",
    },
  ];

  return (
    <section id="features" className="py-24 px-6 relative max-w-7xl mx-auto">
      {/* Recruiter Section */}
      <div id="recruiters" className="mb-24">
        <div className="text-center md:text-left max-w-3xl mb-16">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-sky-600 mb-3">Recruiting Infrastructure</h2>
          <h3 className="text-3xl sm:text-4xl font-extrabold text-zinc-900 tracking-tight">Built to scale for hiring managers.</h3>
          <p className="text-zinc-600 mt-4">
            Manage candidates, schedule multiple rounds of interviews, and audit system state changes transactionally.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {recruiterFeatures.map((feat, idx) => (
            <motion.div 
              key={idx}
              whileHover={{ y: -8 }}
              className="p-8 rounded-2xl border border-zinc-200 bg-white/70 shadow-sm glow-hover relative group overflow-hidden"
            >
              <div className="w-12 h-12 rounded-xl bg-zinc-100 flex items-center justify-center mb-6">
                {feat.icon}
              </div>
              <h4 className="text-xl font-bold text-zinc-900 mb-3">{feat.title}</h4>
              <p className="text-zinc-650 text-sm leading-relaxed">{feat.desc}</p>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Candidate Section */}
      <div id="candidates" className="border-t border-zinc-200 pt-24">
        <div className="text-center md:text-right max-w-3xl md:ml-auto mb-16">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-emerald-600 mb-3">Job Seeker Experience</h2>
          <h3 className="text-3xl sm:text-4xl font-extrabold text-zinc-900 tracking-tight">No black holes. Full transparency.</h3>
          <p className="text-zinc-650 mt-4">
            Submit unique applications per job posting, track pipeline stages in real-time, and securely manage reset workflows.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {candidateFeatures.map((feat, idx) => (
            <motion.div 
              key={idx}
              whileHover={{ y: -8 }}
              className="p-8 rounded-2xl border border-zinc-200 bg-white/70 shadow-sm glow-hover relative group overflow-hidden"
            >
              <div className="w-12 h-12 rounded-xl bg-zinc-100 flex items-center justify-center mb-6">
                {feat.icon}
              </div>
              <h4 className="text-xl font-bold text-zinc-900 mb-3">{feat.title}</h4>
              <p className="text-zinc-650 text-sm leading-relaxed">{feat.desc}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
