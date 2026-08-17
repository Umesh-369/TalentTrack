"use client";

import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Users, CheckCircle2, XCircle, Clock, Calendar, 
  ArrowRight, ShieldCheck, RefreshCw, LogOut, Briefcase
} from "lucide-react";
import Link from "next/link";

interface Application {
  id: number;
  candidateName: string;
  candidateEmail: string;
  jobPostingId: number;
  jobPostingTitle: string;
  status: "APPLIED" | "SHORTLISTED" | "INTERVIEW" | "HIRED" | "REJECTED";
  appliedAt: string;
  version: number;
}

interface AuditLog {
  id: number;
  actorType: string;
  actorId: number;
  action: string;
  entityType: string;
  entityId: number;
  timestamp: string;
  metadata: Record<string, string>;
}

const mockApplications: Application[] = [
  { id: 1, candidateName: "Jane Doe", candidateEmail: "jane@gmail.com", jobPostingId: 101, jobPostingTitle: "Lead Java Developer", status: "APPLIED", appliedAt: "2026-08-09T10:00:00", version: 0 },
  { id: 2, candidateName: "Alex Smith", candidateEmail: "alex@gmail.com", jobPostingId: 101, jobPostingTitle: "Lead Java Developer", status: "SHORTLISTED", appliedAt: "2026-08-09T09:30:00", version: 1 },
  { id: 3, candidateName: "Robert Johnson", candidateEmail: "robert@gmail.com", jobPostingId: 102, jobPostingTitle: "Frontend Architect", status: "INTERVIEW", appliedAt: "2026-08-09T08:15:00", version: 2 },
  { id: 4, candidateName: "Emily Davis", candidateEmail: "emily@gmail.com", jobPostingId: 102, jobPostingTitle: "Frontend Architect", status: "HIRED", appliedAt: "2026-08-08T14:00:00", version: 1 },
];

const mockLogs: AuditLog[] = [
  { id: 1, actorType: "CANDIDATE", actorId: 201, action: "APPLICATION_SUBMITTED", entityType: "Application", entityId: 1, timestamp: "2026-08-09T10:00:00Z", metadata: { candidateEmail: "jane@gmail.com", jobTitle: "Lead Java Developer" } },
  { id: 2, actorType: "RECRUITER", actorId: 1, action: "APPLICATION_STATUS_UPDATED", entityType: "Application", entityId: 2, timestamp: "2026-08-09T10:15:00Z", metadata: { status: "SHORTLISTED" } },
  { id: 3, actorType: "SYSTEM", actorId: 0, action: "JOB_EXPIRED", entityType: "JobPosting", entityId: 105, timestamp: "2026-08-09T00:00:00Z", metadata: { title: "Python Intern" } },
];

export default function RecruiterDashboard() {
  const [applications, setApplications] = useState<Application[]>(mockApplications);
  const [logs, setLogs] = useState<AuditLog[]>(mockLogs);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    // Load local auth token if present
    const savedToken = localStorage.getItem("recruiterToken");
    if (savedToken) setToken(savedToken);
  }, []);

  // Update Status Service Call
  const handleTransition = async (id: number, targetStatus: string, currentVersion: number) => {
    setErrorMessage(null);
    setSuccessMessage(null);

    // Simulate stale write check (Optimistic Locking - Invariant 5)
    const currentApp = applications.find(a => a.id === id);
    if (currentApp && currentApp.version !== currentVersion) {
      setErrorMessage("Conflict (409): Stale write detected. Another recruiter modified this application. Please refresh.");
      return;
    }

    try {
      // Real API invocation if authenticated
      if (token) {
        const res = await fetch(`/api/applications/${id}/status?status=${targetStatus}`, {
          method: "PATCH",
          headers: {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        });
        if (res.status === 409) {
          setErrorMessage("Conflict (409): Invalid transition or stale write.");
          return;
        }
        if (!res.ok) {
          const errData = await res.json();
          setErrorMessage(errData.message || "Failed to update status");
          return;
        }
        const updated = await res.json();
        setApplications(prev => prev.map(a => a.id === id ? updated : a));
        setSuccessMessage(`Updated application status to ${targetStatus}`);
        return;
      }

      // Mock Client-Side State Machine & Version increment
      const updatedList = applications.map(app => {
        if (app.id === id) {
          return {
            ...app,
            status: targetStatus as any,
            version: app.version + 1
          };
        }
        return app;
      });
      setApplications(updatedList);
      setSuccessMessage(`Mock state transition succeeded: status changed to ${targetStatus}`);
      
      // Append Mock Audit Log entry (Invariant 11)
      const newLog: AuditLog = {
        id: Date.now(),
        actorType: "RECRUITER",
        actorId: 1,
        action: "APPLICATION_STATUS_UPDATED",
        entityType: "Application",
        entityId: id,
        timestamp: new Date().toISOString(),
        metadata: { status: targetStatus }
      };
      setLogs(prev => [newLog, ...prev]);

    } catch (err: any) {
      setErrorMessage("Connection failed. Fallback to mock update failed.");
    }
  };

  const columns: { name: string; status: Application["status"]; color: string }[] = [
    { name: "Applied", status: "APPLIED", color: "border-sky-200 bg-sky-50/20" },
    { name: "Shortlisted", status: "SHORTLISTED", color: "border-cyan-200 bg-cyan-50/20" },
    { name: "Interview", status: "INTERVIEW", color: "border-sky-250 bg-sky-100/10" },
    { name: "Hired", status: "HIRED", color: "border-emerald-200 bg-emerald-50/20" },
    { name: "Rejected", status: "REJECTED", color: "border-rose-200 bg-rose-50/20" }
  ];

  // Helper to determine if a transition edge is valid (Invariant 3)
  const getValidTransitions = (current: Application["status"]) => {
    switch (current) {
      case "APPLIED":
        return ["SHORTLISTED", "REJECTED"];
      case "SHORTLISTED":
        return ["INTERVIEW", "REJECTED"];
      case "INTERVIEW":
        return ["HIRED", "REJECTED"];
      default:
        return []; // Hired or Rejected are terminal (Invariant 4)
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#f8fafc] text-zinc-800">
      {/* Dashboard Navbar */}
      <header className="glassmorphism border-b border-zinc-200 px-8 py-5 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-3">
          <Briefcase className="h-6 w-6 text-sky-600" />
          <h1 className="text-xl font-bold tracking-wide text-zinc-900">TalentTrack <span className="text-sky-600 font-semibold">Workspace</span></h1>
        </div>
        <div className="flex items-center gap-6">
          <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-zinc-100 border border-zinc-200 text-xs text-zinc-650">
            <ShieldCheck className="h-4 w-4 text-sky-600" />
            <span>Recruiter Role: Company Tenant #1</span>
          </div>
          <Link href="/" className="flex items-center gap-2 text-sm text-zinc-555 hover:text-zinc-900 transition-colors">
            <LogOut className="h-4 w-4" />
            <span>Sign Out</span>
          </Link>
        </div>
      </header>

      {/* Workspace Grid */}
      <div className="flex-grow grid grid-cols-1 lg:grid-cols-4 p-8 gap-8">
        
        {/* Kanban Board Area (Columns 1-3) */}
        <main className="lg:col-span-3 space-y-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-2xl font-extrabold text-zinc-900">Recruitment Pipeline</h2>
              <p className="text-zinc-550 text-sm mt-1">Manage active applicants and advance their hiring stages.</p>
            </div>
            <button 
              onClick={() => {
                setApplications(mockApplications);
                setLogs(mockLogs);
                setErrorMessage(null);
                setSuccessMessage("Workspace reset to mock defaults");
              }}
              className="flex items-center gap-2 text-xs bg-white border border-zinc-200 hover:border-zinc-300 hover:text-zinc-900 text-zinc-600 px-4 py-2.5 rounded-lg transition-all duration-300 shadow-sm"
            >
              <RefreshCw className="h-3 w-3" />
              Reset Demo States
            </button>
          </div>

          {/* Feedback Alerts */}
          {errorMessage && (
            <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-sm font-semibold">
              ⚠️ {errorMessage}
            </div>
          )}
          {successMessage && (
            <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50 text-emerald-800 text-sm font-semibold">
              ✨ {successMessage}
            </div>
          )}

          {/* Kanban Columns */}
          <div className="grid grid-cols-1 sm:grid-cols-5 gap-6 items-start">
            {columns.map((col, idx) => {
              const colApps = applications.filter(a => a.status === col.status);
              return (
                <div key={idx} className={`rounded-2xl border ${col.color} p-4 min-h-[500px]`}>
                  <div className="flex items-center justify-between mb-4 border-b border-zinc-100 pb-2">
                    <span className="font-bold text-sm text-zinc-700">{col.name}</span>
                    <span className="w-5 h-5 flex items-center justify-center rounded-full bg-zinc-100 text-zinc-650 text-xs font-semibold">
                      {colApps.length}
                    </span>
                  </div>

                  <div className="space-y-4">
                    <AnimatePresence mode="popLayout">
                      {colApps.map((app) => {
                        const validMoves = getValidTransitions(app.status);
                        const isTerminal = validMoves.length === 0;

                        return (
                          <motion.div
                            key={app.id}
                            layoutId={`card-${app.id}`}
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.95 }}
                            className="p-4 rounded-xl border border-zinc-200 bg-white shadow-sm relative"
                          >
                            <h4 className="font-bold text-sm text-zinc-900">{app.candidateName}</h4>
                            <p className="text-zinc-500 text-xs truncate mt-0.5">{app.candidateEmail}</p>
                            <p className="text-sky-650 text-xs font-semibold mt-3">{app.jobPostingTitle}</p>
                            
                            <div className="flex items-center gap-2 mt-4 pt-3 border-t border-zinc-100">
                              <span className="text-[10px] text-zinc-500 font-semibold uppercase">v{app.version}</span>
                              <span className="text-[10px] text-zinc-400">•</span>
                              <span className="text-[10px] text-zinc-500 truncate flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {new Date(app.appliedAt).toLocaleDateString()}
                              </span>
                            </div>                              {/* Transition Buttons */}
                            {!isTerminal && (
                              <div className="flex items-center flex-wrap gap-2 mt-4">
                                {validMoves.map((target) => (
                                  <button
                                    key={target}
                                    onClick={() => handleTransition(app.id, target, app.version)}
                                    className={`text-[10px] font-bold px-2 py-1.5 rounded transition-all duration-300 ${
                                      target === "REJECTED" 
                                        ? "bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200" 
                                        : "bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200"
                                    }`}
                                  >
                                    Move to {target}
                                  </button>
                                ))}
                              </div>
                            )}

                            {isTerminal && (
                              <div className="mt-4 p-2 rounded bg-zinc-50 border border-zinc-200 text-[10px] text-center text-zinc-500 font-semibold uppercase">
                                Locked Stage ({app.status})
                              </div>
                            )}
                          </motion.div>
                        );
                      })}
                    </AnimatePresence>
                  </div>
                </div>
              );
            })}
          </div>
        </main>

        {/* Audit Log Panel (Column 4) */}
        <aside className="glassmorphism rounded-2xl p-6 h-fit border border-zinc-200">
          <h2 className="text-lg font-bold text-zinc-900 mb-2 flex items-center gap-2">
            <Users className="h-5 w-5 text-sky-600" />
            <span>Activity Trail</span>
          </h2>
          <p className="text-zinc-500 text-xs mb-6">Immutable audit records written transactionally.</p>

          <div className="space-y-6 max-h-[600px] overflow-y-auto pr-2">
            {logs.map((log) => (
              <div key={log.id} className="relative pl-6 border-l border-zinc-200 pb-1">
                {/* Visual Connector Dot */}
                <div className="absolute left-[-4.5px] top-1.5 w-2.5 h-2.5 rounded-full bg-sky-500/30 border border-sky-500" />
                
                <p className="text-xs font-bold text-zinc-900">{log.action.replace("_", " ")}</p>
                <p className="text-[10px] text-zinc-500 mt-1">
                  Actor: <span className="text-zinc-700 font-semibold">{log.actorType}</span> ({log.actorId})
                </p>
                
                {log.metadata && Object.keys(log.metadata).length > 0 && (
                  <div className="mt-2 p-2 rounded bg-zinc-50 border border-zinc-150 text-[10px] text-zinc-650 space-y-1">
                    {Object.entries(log.metadata).map(([key, val]) => (
                      <div key={key} className="flex justify-between">
                        <span className="text-zinc-550 font-semibold">{key}:</span>
                        <span>{val}</span>
                      </div>
                    ))}
                  </div>
                )}
                
                <span className="text-[9px] text-zinc-500 block mt-2">
                  {new Date(log.timestamp).toLocaleTimeString()}
                </span>
              </div>
            ))}
          </div>
        </aside>

      </div>
    </div>
  );
}
