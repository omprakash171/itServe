export const BASE = 'http://localhost:8081/api';
export const state = { pass: 0, fail: 0, results: [] };

export async function req(method, path, token, body, isForm) {
  const headers = {};
  if (token) headers.Authorization = 'Bearer ' + token;
  let payload;
  if (isForm) { payload = body; }
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  const res = await fetch(BASE + path, { method, headers, body: payload });
  let json = null;
  try { json = await res.json(); } catch {}
  return { status: res.status, json };
}

export async function check(name, fn) {
  try {
    const ok = await fn();
    if (ok === true || ok === undefined) { state.pass++; state.results.push(['PASS', name, '']); }
    else { state.fail++; state.results.push(['FAIL', name, String(ok)]); }
  } catch (e) {
    state.fail++; state.results.push(['FAIL', name, e.message]);
  }
}

export function expectStatus(r, ...codes) {
  if (codes.includes(r.status)) return true;
  return `expected ${codes.join('/')} got ${r.status}: ${JSON.stringify(r.json)?.slice(0, 200)}`;
}

export function report() {
  console.log('\n' + '='.repeat(78));
  for (const [st, name, detail] of state.results) {
    console.log(`${st}  ${name}${detail ? '  ->  ' + detail : ''}`);
  }
  console.log('='.repeat(78));
  console.log(`TOTAL: ${state.pass + state.fail}  PASS: ${state.pass}  FAIL: ${state.fail}`);
  return state.fail;
}

export const PW = 'Password@123';
export const uniq = Date.now();
export const ctx = {};
