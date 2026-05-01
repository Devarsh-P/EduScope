export const API = "http://localhost:8081/api";

export async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`GET failed: ${res.status}`);
  }
  return res.json();
}

export async function postJson(url, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(`POST failed: ${res.status}`);
  }

  return res.json();
}
