"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Briefcase, User, Building2, Lock, Mail, ArrowRight, Eye, EyeOff, Loader2, Sparkles, CheckCircle2, AlertCircle } from "lucide-react";
import { loginApi, authStorage } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [role, setRole] = useState<"candidate" | "recruiter">("candidate");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      const response = await loginApi(role, { email, password });
      authStorage.saveSession(response);
      setSuccess("Login successful! Redirecting to dashboard...");
      
      setTimeout(() => {
        if (role === "recruiter") {
          router.push("/recruiter/dashboard");
        } else {
          router.push("/candidate/dashboard");
        }
      }, 800);
    } catch (err: any) {
      setError(err?.message || "Invalid credentials. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = (demoRole: "candidate" | "recruiter") => {
    setRole(demoRole);
    const demoData = demoRole === "recruiter" 
      ? { accessToken: "demo-token", refreshToken: "demo-refresh", email: "recruiter@demo.com", role: "RECRUITER", userId: 1, companyId: 101 }
      : { accessToken: "demo-token", refreshToken: "demo-refresh", email: "candidate@demo.com", role: "CANDIDATE", userId: 2, companyId: null };
    
    authStorage.saveSession(demoData);
    setSuccess(`Logging in as Demo ${demoRole === "recruiter" ? "Recruiter" : "Candidate"}...`);
    setTimeout(() => {
      router.push(demoRole === "recruiter" ? "/recruiter/dashboard" : "/candidate/dashboard");
    }, 600);
  };

  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden bg-gradient-to-br from-zinc-50 via-sky-50/30 to-zinc-100">
      {/* Background Orbs */}
      <div className="absolute top-1/4 -left-20 w-96 h-96 bg-sky-400/15 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-indigo-500/10 rounded-full blur-[100px] pointer-events-none" />

      {/* Top Header Logo */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center z-10">
        <Link href="/" className="inline-flex items-center gap-2 font-extrabold text-2xl text-zinc-900 tracking-wide mb-2">
          <Briefcase className="h-7 w-7 text-sky-600" />
          <span>Talent<span className="text-sky-600">Track</span></span>
        </Link>
        <h2 className="mt-2 text-center text-3xl font-extrabold text-zinc-900 tracking-tight">
          Welcome back
        </h2>
        <p className="mt-2 text-center text-sm text-zinc-600">
          Sign in to access your recruitment portal
        </p>
      </div>

      {/* Main Form Card */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="mt-8 sm:mx-auto sm:w-full sm:max-w-md z-10 px-4 sm:px-0"
      >
        <div className="bg-white/80 backdrop-blur-xl py-8 px-6 shadow-2xl rounded-2xl border border-zinc-200/80 sm:px-10">
          
          {/* Role Toggle Tabs */}
          <div className="grid grid-cols-2 gap-2 p-1.5 bg-zinc-100/90 rounded-xl mb-6">
            <button
              type="button"
              onClick={() => { setRole("candidate"); setError(null); }}
              className={`flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                role === "candidate"
                  ? "bg-white text-sky-700 shadow-md"
                  : "text-zinc-600 hover:text-zinc-900"
              }`}
            >
              <User className="w-4 h-4" />
              <span>Job Seeker</span>
            </button>

            <button
              type="button"
              onClick={() => { setRole("recruiter"); setError(null); }}
              className={`flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                role === "recruiter"
                  ? "bg-white text-sky-700 shadow-md"
                  : "text-zinc-600 hover:text-zinc-900"
              }`}
            >
              <Building2 className="w-4 h-4" />
              <span>Employer</span>
            </button>
          </div>

          {/* Feedback Alerts */}
          {error && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-4 p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </motion.div>
          )}

          {success && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-4 p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-medium flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{success}</span>
            </motion.div>
          )}

          {/* Form */}
          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 uppercase tracking-wider mb-1.5">
                Work Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={role === "recruiter" ? "hr@company.com" : "you@example.com"}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-zinc-200 focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-600 text-sm transition-all"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-zinc-700 uppercase tracking-wider">
                  Password
                </label>
                <a href="#" className="text-xs text-sky-600 hover:text-sky-500 font-semibold">
                  Forgot?
                </a>
              </div>
              <div className="relative">
                <Lock className="w-5 h-5 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-10 py-2.5 rounded-xl border border-zinc-200 focus:outline-none focus:ring-2 focus:ring-sky-500/20 focus:border-sky-600 text-sm transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-semibold text-sm transition-all shadow-lg shadow-sky-600/25 active:scale-98 disabled:opacity-70"
            >
              {loading ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <>
                  <span>Sign In as {role === "recruiter" ? "Employer" : "Candidate"}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Login Option */}
          <div className="mt-6 pt-6 border-t border-zinc-100">
            <div className="flex items-center justify-between text-xs text-zinc-500 mb-3">
              <span>Quick Test Drive:</span>
              <span className="flex items-center gap-1 text-sky-600 font-semibold">
                <Sparkles className="w-3.5 h-3.5" /> Instant Demo
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleDemoLogin("candidate")}
                className="px-3 py-2 rounded-lg bg-zinc-50 hover:bg-zinc-100 border border-zinc-200 text-xs font-semibold text-zinc-700 transition-colors text-center"
              >
                Demo Candidate
              </button>
              <button
                type="button"
                onClick={() => handleDemoLogin("recruiter")}
                className="px-3 py-2 rounded-lg bg-zinc-50 hover:bg-zinc-100 border border-zinc-200 text-xs font-semibold text-zinc-700 transition-colors text-center"
              >
                Demo Employer
              </button>
            </div>
          </div>

          {/* Footer link to Register */}
          <div className="mt-6 text-center text-xs text-zinc-600">
            Don&apos;t have an account?{" "}
            <Link href={`/register${role === "recruiter" ? "?role=recruiter" : ""}`} className="font-bold text-sky-600 hover:text-sky-500">
              Create an account
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
