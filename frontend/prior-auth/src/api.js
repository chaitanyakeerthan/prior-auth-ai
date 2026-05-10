/**
 * API client for the XAIP Spring Boot backend.
 */

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

/** Get stored JWT token */
function getToken() {
  return localStorage.getItem("xaip_token");
}

/** Build headers with optional JWT */
function authHeaders() {
  const token = getToken();
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  return headers;
}

/** Generic error extractor */
async function handleResponse(res) {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || body.error || `Request failed (${res.status})`);
  }
  return res.json();
}

// ── Auth ────────────────────────────────────────────────────

export async function registerUser({ fullName, email, password }) {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ fullName, email, password }),
  });
  return handleResponse(res);
}

export async function loginUser({ email, password }) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  return handleResponse(res);
}

export async function googleLoginUser(token) {
  const res = await fetch(`${API_BASE}/auth/google`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });
  return handleResponse(res);
}

// ── Prediction ──────────────────────────────────────────────

export async function submitPrediction({ age, diagnosis, treatment, provider, priority, clinicalNotes, document }) {
  const formData = new FormData();
  formData.append("age", age);
  formData.append("diagnosis", diagnosis);
  formData.append("treatment", treatment);
  if (provider) formData.append("provider", provider);
  if (priority) formData.append("priority", priority);
  if (clinicalNotes) formData.append("clinicalNotes", clinicalNotes);
  if (document) formData.append("document", document);

  const token = getToken();
  const headers = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;
  // Let the browser set Content-Type for FormData

  const res = await fetch(`${API_BASE}/predict`, {
    method: "POST",
    headers,
    body: formData,
  });
  return handleResponse(res);
}

export async function fetchHistory() {
  const res = await fetch(`${API_BASE}/history`, {
    headers: authHeaders(),
  });
  return handleResponse(res);
}
