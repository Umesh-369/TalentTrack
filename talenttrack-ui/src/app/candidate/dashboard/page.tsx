"use client";

import { useState, useEffect, useRef } from "react";
import { motion } from "framer-motion";
import { 
  Briefcase, Search, Check, AlertTriangle, ArrowRight, 
  MapPin, Clock, LogOut, Compass, HelpCircle, Layers
} from "lucide-react";
import Link from "next/link";

interface JobPosting {
  id: number;
  title: string;
  description: string;
  location: string;
  remote: boolean;
  experienceMin: number;
  experienceMax: number;
  status: "DRAFT" | "PUBLISHED" | "CLOSED" | "EXPIRED";
  expiresAt: string | null;
  companyName: string;
}

interface Application {
  id: number;
  jobPostingId: number;
  jobPostingTitle: string;
  companyName: string;
  status: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "HIRED" | "REJECTED";
  appliedAt: string;
}

const mockJobs: JobPosting[] = [
  { id: 101, title: "Lead Java Developer", description: "Design next-gen banking REST APIs using Spring Boot and Java 17.", location: "Boston, MA", remote: false, experienceMin: 5, experienceMax: 10, status: "PUBLISHED", expiresAt: "2026-09-30T00:00:00", companyName: "ApexCore" },
  { id: 102, title: "Frontend Architect", description: "Lead Next.js 15 app router migrations with Tailwind CSS and Framer Motion.", location: "Remote", remote: true, experienceMin: 6, experienceMax: 12, status: "PUBLISHED", expiresAt: "2026-10-15T00:00:00", companyName: "NovaLink" },
  { id: 103, title: "Senior DevOps Engineer", description: "Scale Kubernetes clusters, Helm charts, and automated Terraform workspaces.", location: "Austin, TX", remote: true, experienceMin: 4, experienceMax: 8, status: "PUBLISHED", expiresAt: "2026-08-08T00:00:00", companyName: "StellarWeb" }, // Expired
  { id: 104, title: "Product Designer", description: "Build slick wireframes, interactive user flows, and high fidelity mockups.", location: "San Francisco, CA", remote: false, experienceMin: 3, experienceMax: 6, status: "CLOSED", expiresAt: "2026-09-01T00:00:00", companyName: "Vanguard" }, // Closed
  { id: 105, title: "Junior Python developer", description: "Build scraper microservices and Flask backend endpoints.", location: "Denver, CO", remote: true, experienceMin: 1, experienceMax: 3, status: "PUBLISHED", expiresAt: "2026-09-10T00:00:00", companyName: "DevLogic" },
];

const mockApplications: Application[] = [
  { id: 1, jobPostingId: 101, jobPostingTitle: "Lead Java Developer", companyName: "ApexCore", status: "APPLIED", appliedAt: "2026-08-09T10:00:00" },
  { id: 2, jobPostingId: 102, jobPostingTitle: "Frontend Architect", companyName: "NovaLink", status: "INTERVIEW", appliedAt: "2026-08-07T14:30:00" },
];

export default function CandidateDashboard() {
  const [jobs, setJobs] = useState<JobPosting[]>(mockJobs);
  const [myApplications, setMyApplications] = useState<Application[]>(mockApplications);
  const [selectedApp, setSelectedApp] = useState<Application | null>(mockApplications[1]); // Default select second app (Interview status)
  
  // Search state
  const [locationQuery, setLocationQuery] = useState("");
  const [remoteFilter, setRemoteFilter] = useState<boolean | null>(null);
  
  // Alerts state
  const [alertMessage, setAlertMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const carouselRef = useRef<HTMLDivElement>(null);

  // Handle Application Submission (Enforcing Invariants 9 & 10)
  const handleApply = (job: JobPosting) => {
    setAlertMessage(null);

    // Invariant 10: Cannot apply to closed or expired jobs
    if (job.status !== "PUBLISHED") {
      setAlertMessage({ type: "error", text: `Apply rejected: Job posting is currently ${job.status}` });
      return;
    }
    if (job.expiresAt && new Date(job.expiresAt) < new Date()) {
      setAlertMessage({ type: "error", text: "Apply rejected: Job posting is expired." });
      return;
    }

    // Invariant 9: Unique candidate per job
    const alreadyApplied = myApplications.some(app => app.jobPostingId === job.id);
    if (alreadyApplied) {
      setAlertMessage({ type: "error", text: "Apply rejected: You have already submitted an application to this job posting." });
      return;
    }

    // Create new application
    const newApp: Application = {
      id: Date.now(),
      jobPostingId: job.id,
      jobPostingTitle: job.title,
      companyName: job.companyName,
      status: "APPLIED",
      appliedAt: new Date().toISOString(),
    };

    setMyApplications(prev => [...prev, newApp]);
    setSelectedApp(newApp);
    setAlertMessage({ type: "success", text: `Successfully applied to ${job.title} at ${job.companyName}!` });
  };

  // Filtered jobs search
  const filteredJobs = jobs.filter(job => {
    const matchesLoc = job.location.toLowerCase().includes(locationQuery.toLowerCase());
    const matchesRemote = remoteFilter === null ? true : job.remote === remoteFilter;
    return matchesLoc && matchesRemote;
  });

  // Stages of timeline (Invariant 3)
  const timelineStages: { status: Application["status"]; desc: string }[] = [
    { status: "APPLIED", desc: "Application submitted and received by HR." },
    { status: "SHORTLISTED", desc: "Resume passed preliminary reviews." },
    { status: "INTERVIEW", desc: "Recruiter scheduled interviews and evaluations." },
    { status: "HIRED", desc: "Offer extended! Congratulations!" },
  ];

  const getStageIndex = (status: Application["status"]) => {
    if (status === "REJECTED") return -1;
    return timelineStages.findIndex(s => s.status === status);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#f8fafc] text-zinc-800">
      
      {/* Dashboard Navbar */}
      <header className="glassmorphism border-b border-zinc-200 px-8 py-5 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-3">
          <Briefcase className="h-6 w-6 text-sky-600" />
          <h1 className="text-xl font-bold tracking-wide text-zinc-900">TalentTrack <span className="text-emerald-600 font-semibold">Portal</span></h1>
        </div>
        <div className="flex items-center gap-6">
          <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-zinc-100 border border-zinc-200 text-xs text-zinc-650">
            <Compass className="h-4 w-4 text-emerald-600" />
            <span>Candidate: Jane Doe</span>
          </div>
          <Link href="/" className="flex items-center gap-2 text-sm text-zinc-555 hover:text-zinc-900 transition-colors">
            <LogOut className="h-4 w-4" />
            <span>Sign Out</span>
          </Link>
        </div>
      </header>

      {/* Main Workspace Layout */}
      <div className="flex-grow p-8 max-w-7xl mx-auto w-full space-y-12">
        
        {/* Banner Feedback Alerts */}
        {alertMessage && (
          <div 
            className={`p-4 rounded-xl border text-sm font-semibold flex items-center gap-3 ${
              alertMessage.type === "success" 
                ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                : "border-rose-200 bg-rose-50 text-rose-800"
            }`}
          >
            {alertMessage.type === "success" ? <Check className="h-5 w-5" /> : <AlertTriangle className="h-5 w-5" />}
            <span>{alertMessage.text}</span>
          </div>
        )}

        {/* Snap Scroll Job Recommendations Carousel Section */}
        <div>
          <h2 className="text-xl font-extrabold text-zinc-900 mb-2">Recommended Opportunities</h2>
          <p className="text-zinc-500 text-xs mb-6">Handpicked listings aligning with your profile. Snap scroll horizontally to browse.</p>

          <div 
            ref={carouselRef}
            className="flex gap-6 overflow-x-auto snap-x snap-mandatory pb-4 scrollbar-thin scrollbar-thumb-zinc-300"
          >
            {jobs.filter(j => j.status === "PUBLISHED" && (!j.expiresAt || new Date(j.expiresAt) > new Date())).map((job) => (
              <div 
                key={job.id} 
                className="snap-start shrink-0 w-80 p-6 rounded-2xl border border-zinc-200 bg-white/70 shadow-sm flex flex-col justify-between"
              >
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-100">
                    {job.companyName}
                  </span>
                  <h3 className="font-extrabold text-lg text-zinc-900 mt-4">{job.title}</h3>
                  <p className="text-zinc-500 text-xs mt-1 flex items-center gap-1">
                    <MapPin className="h-3 w-3" />
                    {job.location}
                  </p>
                  <p className="text-zinc-600 text-sm mt-4 line-clamp-3 leading-relaxed">{job.description}</p>
                </div>

                <div className="mt-6 pt-4 border-t border-zinc-100 flex items-center justify-between">
                  <span className="text-[10px] text-zinc-500 font-semibold">{job.experienceMin}-{job.experienceMax} Yrs Exp</span>
                  <button 
                    onClick={() => handleApply(job)}
                    className="flex items-center gap-1.5 text-xs font-bold text-emerald-700 hover:text-white bg-emerald-50 hover:bg-emerald-600 border border-emerald-200 hover:border-emerald-500 px-3.5 py-2 rounded-lg transition-all duration-300 active:scale-95"
                  >
                    <span>Apply Now</span>
                    <ArrowRight className="h-3 w-3" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Core Tracker Workspace (Timeline Tracking and Job Search List) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Applications list & Stage tracker (Columns 1-7) */}
          <div className="lg:col-span-7 space-y-6">
            <h2 className="text-xl font-extrabold text-zinc-900">Your Applications & Status</h2>
            <p className="text-zinc-500 text-xs">Select any application to view its interactive hiring progress timeline.</p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {myApplications.map((app) => (
                <div 
                  key={app.id}
                  onClick={() => setSelectedApp(app)}
                  className={`p-5 rounded-2xl border transition-all duration-300 cursor-pointer ${
                    selectedApp?.id === app.id 
                      ? "border-emerald-500 bg-emerald-50/50" 
                      : "border-zinc-200 bg-white hover:border-zinc-300 shadow-sm"
                  }`}
                >
                  <h3 className="font-extrabold text-sm text-zinc-900">{app.jobPostingTitle}</h3>
                  <p className="text-zinc-550 text-xs mt-0.5">{app.companyName}</p>
                  
                  <div className="flex items-center justify-between mt-6">
                    <span className="text-[10px] text-zinc-500 font-semibold">
                      Applied {new Date(app.appliedAt).toLocaleDateString()}
                    </span>
                    <span className={`text-[10px] font-bold uppercase tracking-wider px-2.5 py-1 rounded-lg border ${
                      app.status === "HIRED" ? "bg-emerald-100 text-emerald-700 border-emerald-250" :
                      app.status === "REJECTED" ? "bg-rose-100 text-rose-700 border-rose-250" :
                      app.status === "INTERVIEW" ? "bg-cyan-100 text-cyan-700 border-cyan-250" :
                      "bg-sky-100 text-sky-700 border-sky-250"
                    }`}>
                      {app.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Selected Application Timeline Tracker */}
            {selectedApp && (
              <div className="p-6 rounded-2xl border border-zinc-200 bg-white shadow-sm mt-6">
                <h3 className="text-sm font-bold text-zinc-500 uppercase tracking-wider mb-6">
                  Workflow Timeline: <span className="text-zinc-900 normal-case font-extrabold">{selectedApp.jobPostingTitle} ({selectedApp.companyName})</span>
                </h3>

                {selectedApp.status === "REJECTED" ? (
                  <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-xs flex items-center gap-3">
                    <AlertTriangle className="h-5 w-5" />
                    <span>This application was closed with status: REJECTED. Don't worry, keep browsing other matches!</span>
                  </div>
                ) : (
                  <div className="relative pl-6 space-y-8 before:absolute before:left-[11px] before:top-2 before:bottom-2 before:w-[2px] before:bg-zinc-200">
                    {timelineStages.map((stage, idx) => {
                      const curStageIdx = getStageIndex(selectedApp.status);
                      const isCompleted = idx < curStageIdx;
                      const isActive = idx === curStageIdx;
                      
                      return (
                        <div key={idx} className="relative flex gap-6 items-start">
                          {/* Timeline Dot Indicator */}
                          <div className={`absolute left-[-21px] top-1 w-6 h-6 rounded-full flex items-center justify-center border text-[10px] font-bold z-10 transition-all duration-300 ${
                            isCompleted ? "bg-emerald-600 border-emerald-600 text-white" :
                            isActive ? "bg-emerald-500 border-emerald-500 text-white shadow-md shadow-emerald-500/20" :
                            "bg-zinc-100 border-zinc-250 text-zinc-400"
                          }`}>
                            {isCompleted ? <Check className="h-3 w-3" /> : idx + 1}
                          </div>

                          <div>
                            <p className={`text-sm font-extrabold transition-colors duration-300 ${
                              isActive || isCompleted ? "text-zinc-900" : "text-zinc-400"
                            }`}>
                              {stage.status}
                            </p>
                            <p className="text-zinc-500 text-xs mt-1">{stage.desc}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Job Search Directory (Columns 8-12) */}
          <aside className="lg:col-span-5 space-y-6">
            <h2 className="text-xl font-extrabold text-zinc-900">Explore Jobs</h2>
            
            {/* Search filter card */}
            <div className="p-5 rounded-2xl border border-zinc-200 bg-white shadow-sm space-y-4">
              {/* Location Input */}
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-550" />
                <input 
                  type="text" 
                  placeholder="Filter by location (e.g. Remote, Boston)..."
                  value={locationQuery}
                  onChange={(e) => setLocationQuery(e.target.value)}
                  className="w-full bg-white border border-zinc-200 hover:border-zinc-300 focus:border-sky-500 focus:ring-1 focus:ring-sky-500/30 rounded-xl pl-10 pr-4 py-2.5 text-sm text-zinc-900 placeholder-zinc-500 transition-all duration-300"
                />
              </div>

              {/* Remote buttons filter */}
              <div className="flex gap-2">
                <button
                  onClick={() => setRemoteFilter(null)}
                  className={`text-xs px-3.5 py-2 rounded-lg font-semibold border transition-all duration-300 ${
                    remoteFilter === null 
                      ? "bg-sky-50 border-sky-500 text-sky-700" 
                      : "bg-white border-zinc-200 text-zinc-600 hover:border-zinc-350"
                  }`}
                >
                  All Formats
                </button>
                <button
                  onClick={() => setRemoteFilter(true)}
                  className={`text-xs px-3.5 py-2 rounded-lg font-semibold border transition-all duration-300 ${
                    remoteFilter === true 
                      ? "bg-sky-50 border-sky-500 text-sky-700" 
                      : "bg-white border-zinc-200 text-zinc-600 hover:border-zinc-350"
                  }`}
                >
                  Remote Only
                </button>
              </div>
            </div>

            {/* Matching Jobs List */}
            <div className="space-y-4 max-h-[500px] overflow-y-auto pr-2">
              {filteredJobs.map((job) => {
                const isClosed = job.status === "CLOSED";
                const isExpired = !!(job.expiresAt && new Date(job.expiresAt) < new Date());
                const isNotActive = !!(isClosed || isExpired);
                const badgeLabel = isClosed ? "CLOSED" : isExpired ? "EXPIRED" : "ACTIVE";

                return (
                  <div 
                    key={job.id} 
                    className="p-5 rounded-2xl border border-zinc-200 bg-white hover:border-zinc-300 hover:bg-zinc-50/50 transition-all duration-300 shadow-sm"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">{job.companyName}</span>
                      <span className={`text-[9px] font-bold px-2 py-0.5 rounded border ${
                        isNotActive 
                          ? "bg-rose-50 border-rose-250 text-rose-700" 
                          : "bg-emerald-50 border-emerald-250 text-emerald-700"
                      }`}>
                        {badgeLabel}
                      </span>
                    </div>

                    <h3 className="font-extrabold text-sm text-zinc-900 mt-3">{job.title}</h3>
                    <p className="text-zinc-550 text-xs mt-1 flex items-center gap-1">
                      <MapPin className="h-3 w-3 text-zinc-500" />
                      {job.location}
                    </p>

                    <p className="text-zinc-600 text-xs mt-3 leading-relaxed line-clamp-2">{job.description}</p>
                    
                    <div className="mt-4 pt-3 border-t border-zinc-100 flex items-center justify-between">
                      <span className="text-[10px] text-zinc-550 font-semibold">{job.experienceMin}-{job.experienceMax} Yrs Exp</span>
                      <button 
                        onClick={() => handleApply(job)}
                        disabled={isNotActive}
                        className={`text-xs font-bold px-3 py-1.5 rounded-lg transition-all duration-300 ${
                          isNotActive 
                            ? "bg-zinc-100 border border-zinc-200 text-zinc-400 cursor-not-allowed" 
                            : "bg-sky-50 text-sky-600 hover:bg-sky-600 hover:text-white border border-sky-200 hover:border-sky-500 active:scale-95"
                        }`}
                      >
                        Apply
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </aside>

        </div>
      </div>
    </div>
  );
}
