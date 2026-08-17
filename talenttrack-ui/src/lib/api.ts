const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  role: "RECRUITER" | "CANDIDATE" | string;
  userId: number;
  companyId?: number | null;
}

export const authStorage = {
  saveSession(auth: AuthResponse) {
    if (typeof window !== "undefined") {
      localStorage.setItem("tt_token", auth.accessToken);
      localStorage.setItem("tt_refresh_token", auth.refreshToken);
      localStorage.setItem("tt_user", JSON.stringify(auth));
    }
  },

  getSession(): AuthResponse | null {
    if (typeof window !== "undefined") {
      const user = localStorage.getItem("tt_user");
      return user ? JSON.parse(user) : null;
    }
    return null;
  },

  clearSession() {
    if (typeof window !== "undefined") {
      localStorage.removeItem("tt_token");
      localStorage.removeItem("tt_refresh_token");
      localStorage.removeItem("tt_user");
    }
  }
};

export async function loginApi(role: "candidate" | "recruiter", data: { email: string; password: string }): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/${role}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data)
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Login failed (${response.status})`);
  }

  return response.json();
}

export async function registerCandidateApi(data: { fullName: string; email: string; password: string; resumeUrl?: string }) {
  const response = await fetch(`${API_BASE_URL}/auth/candidate/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data)
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Registration failed (${response.status})`);
  }

  return response.json();
}

export async function registerRecruiterApi(data: { companyName: string; companySlug: string; email: string; password: string }) {
  const response = await fetch(`${API_BASE_URL}/auth/recruiter/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data)
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `Registration failed (${response.status})`);
  }

  return response.json();
}
