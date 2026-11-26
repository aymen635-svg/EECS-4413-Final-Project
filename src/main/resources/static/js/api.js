async function api(path, options = {}) {
  const res = await fetch(path, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    if (res.status === 401) { location.href = "/login.html"; return; }
    throw new Error(body.message || `Request failed (${res.status})`);
  }
  return body;
}

async function requireUser() {
  try { return await api("/api/users/me"); }
  catch { location.href = "/login.html"; return null; }
}