import { req, check, expectStatus, PW, uniq, ctx } from './lib.mjs';

export async function runPart1() {
  await check('login admin -> 200 + ADMIN role + token, no password leak', async () => {
    const r = await req('POST', '/auth/login', null, { email: 'admin@itserve.com', password: PW });
    const s = expectStatus(r, 200); if (s !== true) return s;
    if (r.json.user.role !== 'ADMIN') return 'role not ADMIN';
    if (JSON.stringify(r.json.user).toLowerCase().includes('password')) return 'PASSWORD LEAKED in response';
    ctx.adminTok = r.json.token; ctx.adminId = r.json.user.id;
    return true;
  });
  await check('login agent1 -> SUPPORT_AGENT', async () => {
    const r = await req('POST', '/auth/login', null, { email: 'agent1@itserve.com', password: PW });
    const s = expectStatus(r, 200); if (s !== true) return s;
    if (r.json.user.role !== 'SUPPORT_AGENT') return 'role not SUPPORT_AGENT';
    ctx.agentTok = r.json.token; ctx.agent1Id = r.json.user.id;
    return true;
  });
  await check('login employee1 -> EMPLOYEE', async () => {
    const r = await req('POST', '/auth/login', null, { email: 'employee1@itserve.com', password: PW });
    const s = expectStatus(r, 200); if (s !== true) return s;
    ctx.empTok = r.json.token; ctx.emp1Id = r.json.user.id;
    return true;
  });
  await check('login wrong password -> 401', async () =>
    expectStatus(await req('POST', '/auth/login', null, { email: 'admin@itserve.com', password: 'wrong' }), 401));
  await check('GET /auth/me with token -> own profile', async () => {
    const r = await req('GET', '/auth/me', ctx.empTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    return r.json.email === 'employee1@itserve.com' || 'wrong email: ' + r.json.email;
  });
  await check('GET /auth/me without token -> 401/403', async () =>
    expectStatus(await req('GET', '/auth/me'), 401, 403));
  await check('register new employee -> can login with EMPLOYEE role', async () => {
    const email = `newuser${uniq}@test.com`;
    const r = await req('POST', '/auth/register', null, { fullName: 'New User', email, password: 'Test@' + uniq, department: 'QA' });
    const s = expectStatus(r, 200, 201); if (s !== true) return s;
    if (r.json.user && r.json.user.role !== 'EMPLOYEE') return 'registered role not EMPLOYEE: ' + r.json.user.role;
    return expectStatus(await req('POST', '/auth/login', null, { email, password: 'Test@' + uniq }), 200);
  });
  await check('register weak password -> 400/422', async () =>
    expectStatus(await req('POST', '/auth/register', null, { fullName: 'X', email: `weak${uniq}@t.com`, password: '123', department: 'QA' }), 400, 422));

  await check('employee GET /users -> 403', async () => expectStatus(await req('GET', '/users', ctx.empTok), 403));
  await check('agent GET /users -> 403', async () => expectStatus(await req('GET', '/users', ctx.agentTok), 403));
  await check('admin GET /users -> 200 with seeded users', async () => {
    const r = await req('GET', '/users', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const list = Array.isArray(r.json) ? r.json : r.json.content;
    return list.length >= 7 || 'only ' + list.length + ' users';
  });
  await check('GET /users?role=SUPPORT_AGENT -> only agents', async () => {
    const r = await req('GET', '/users?role=SUPPORT_AGENT', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const list = Array.isArray(r.json) ? r.json : r.json.content;
    return list.every(u => u.role === 'SUPPORT_AGENT') || 'mixed roles returned';
  });

  await check('admin create user -> can login', async () => {
    const email = `created${uniq}@test.com`;
    ctx.createdEmail = email; ctx.createdPw = 'Create@' + uniq;
    const r = await req('POST', '/users', ctx.adminTok, { fullName: 'Created Agent', email, password: ctx.createdPw, role: 'SUPPORT_AGENT', department: 'IT', skills: ['VPN'] });
    const s = expectStatus(r, 200, 201); if (s !== true) return s;
    ctx.createdUserId = r.json.id;
    return expectStatus(await req('POST', '/auth/login', null, { email, password: ctx.createdPw }), 200);
  });
  await check('admin update user department -> reflected', async () => {
    const r = await req('PUT', `/users/${ctx.createdUserId}`, ctx.adminTok, { fullName: 'Created Agent', email: ctx.createdEmail, department: 'IT-Ops', skills: ['VPN', 'Network'], active: true });
    const s = expectStatus(r, 200); if (s !== true) return s;
    return r.json.department === 'IT-Ops' || 'department not updated: ' + r.json.department;
  });
  await check('admin deactivate user -> deactivated cannot login', async () => {
    const r = await req('DELETE', `/users/${ctx.createdUserId}`, ctx.adminTok);
    const s = expectStatus(r, 200, 204); if (s !== true) return s;
    return expectStatus(await req('POST', '/auth/login', null, { email: ctx.createdEmail, password: ctx.createdPw }), 401, 403);
  });
  await check('admin cannot deactivate self -> 403', async () =>
    expectStatus(await req('DELETE', `/users/${ctx.adminId}`, ctx.adminTok), 403));
  await check('GET /agents/workload (admin) -> 200', async () => expectStatus(await req('GET', '/agents/workload', ctx.adminTok), 200));

  await check('GET /categories -> >=5 categories', async () => {
    const r = await req('GET', '/categories', ctx.empTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    ctx.categories = Array.isArray(r.json) ? r.json : r.json.content;
    return ctx.categories.length >= 5 || 'only ' + ctx.categories.length;
  });
  await check('GET /sla-rules -> CRITICAL 2 / HIGH 4 / MEDIUM 8 / LOW 24', async () => {
    const r = await req('GET', '/sla-rules', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    ctx.slaRules = Array.isArray(r.json) ? r.json : r.json.content;
    const m = Object.fromEntries(ctx.slaRules.map(x => [x.priority, x.resolutionHours]));
    return (m.CRITICAL === 2 && m.HIGH === 4 && m.MEDIUM === 8 && m.LOW === 24) || 'got ' + JSON.stringify(m);
  });
  await check('admin update SLA rule -> applied, then reverted', async () => {
    const crit = ctx.slaRules.find(x => x.priority === 'CRITICAL');
    const r = await req('PUT', `/sla-rules/${crit.id}`, ctx.adminTok, { resolutionHours: 3 });
    const s = expectStatus(r, 200); if (s !== true) return s;
    if (r.json.resolutionHours !== 3) return 'hours not updated';
    return expectStatus(await req('PUT', `/sla-rules/${crit.id}`, ctx.adminTok, { resolutionHours: 2 }), 200);
  });
  await check('SLA rule invalid hours=0 -> 400/422', async () => {
    const crit = ctx.slaRules.find(x => x.priority === 'CRITICAL');
    return expectStatus(await req('PUT', `/sla-rules/${crit.id}`, ctx.adminTok, { resolutionHours: 0 }), 400, 422);
  });
  await check('non-admin update SLA rule -> 403', async () => {
    const crit = ctx.slaRules.find(x => x.priority === 'CRITICAL');
    return expectStatus(await req('PUT', `/sla-rules/${crit.id}`, ctx.empTok, { resolutionHours: 5 }), 403);
  });
}
