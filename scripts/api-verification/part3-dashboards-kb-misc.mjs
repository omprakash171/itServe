import { req, check, expectStatus, uniq, ctx } from './lib.mjs';

export async function runPart3() {
  await check('agent1 unread-count > 0 (assignment notifications)', async () => {
    const r = await req('GET', '/notifications/unread-count', ctx.agentTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const c = r.json.count ?? r.json;
    return c > 0 || 'unread count is ' + c;
  });
  await check('mark one read, then read-all -> unread 0', async () => {
    const list = await req('GET', '/notifications?size=5', ctx.agentTok);
    const s = expectStatus(list, 200); if (s !== true) return s;
    const items = list.json.content ?? list.json;
    const unread = items.find(n => !(n.read ?? n.isRead));
    if (unread) {
      const one = await req('PATCH', `/notifications/${unread.id}/read`, ctx.agentTok);
      const s2 = expectStatus(one, 200, 204); if (s2 !== true) return s2;
    }
    const all = await req('PATCH', '/notifications/read-all', ctx.agentTok);
    const s3 = expectStatus(all, 200, 204); if (s3 !== true) return s3;
    const c = await req('GET', '/notifications/unread-count', ctx.agentTok);
    return (c.json.count ?? c.json) === 0 || 'still unread: ' + JSON.stringify(c.json);
  });

  await check('admin dashboard -> KPIs incl. totalTickets + SLA compliance', async () => {
    const r = await req('GET', '/dashboard/admin', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const j = JSON.stringify(r.json).toLowerCase();
    for (const k of ['totaltickets', 'sla']) if (!j.includes(k)) return 'missing "' + k + '" in dashboard';
    return true;
  });
  await check('agent dashboard -> 200', async () => expectStatus(await req('GET', '/dashboard/agent', ctx.agentTok), 200));
  await check('employee dashboard -> 200', async () => expectStatus(await req('GET', '/dashboard/employee', ctx.empTok), 200));
  await check('role-aware /dashboard/me -> 200 for admin and employee', async () => {
    const a = await req('GET', '/dashboard/me', ctx.adminTok);
    const s1 = expectStatus(a, 200); if (s1 !== true) return s1;
    return expectStatus(await req('GET', '/dashboard/me', ctx.empTok), 200);
  });
  await check('employee GET /dashboard/admin -> 403', async () => expectStatus(await req('GET', '/dashboard/admin', ctx.empTok), 403));

  await check('KB list + search filter', async () => {
    const all = await req('GET', '/articles', ctx.empTok);
    const s = expectStatus(all, 200); if (s !== true) return s;
    const list = Array.isArray(all.json) ? all.json : all.json.content;
    if (!list.length) return 'no articles seeded';
    const sr = await req('GET', '/articles?search=vpn', ctx.empTok);
    const slist = Array.isArray(sr.json) ? sr.json : sr.json.content;
    return (slist.length > 0 && slist.length <= list.length) || 'search returned ' + slist.length + '/' + list.length;
  });
  await check('KB vote helpful -> yes count increments', async () => {
    const list = await req('GET', '/articles', ctx.empTok);
    const article = (Array.isArray(list.json) ? list.json : list.json.content)[0];
    const before = article.helpfulYes;
    const v = await req('POST', `/articles/${article.id}/vote`, ctx.empTok, { helpful: true });
    const s = expectStatus(v, 200); if (s !== true) return s;
    return v.json.helpfulYes === before + 1 || `yes ${before} -> ${v.json.helpfulYes}`;
  });
  await check('agent create + update KB article', async () => {
    const c = await req('POST', '/articles', ctx.agentTok, { title: 'Verify article ' + uniq, category: 'Testing', content: 'Content for verification, long enough to pass validation rules.' });
    const s = expectStatus(c, 200, 201); if (s !== true) return s;
    return expectStatus(await req('PUT', `/articles/${c.json.id}`, ctx.agentTok, { title: 'Verify article updated ' + uniq, category: 'Testing', content: 'Updated content for verification, still long enough to be valid.' }), 200);
  });
  await check('employee create KB article (valid payload) -> 403', async () =>
    expectStatus(await req('POST', '/articles', ctx.empTok, { title: 'Employee should not create', category: 'Testing', content: 'This content is definitely longer than the twenty character minimum.' }), 403));

  await check('GET /api/ratings (admin) -> includes our 5-star rating', async () => {
    const r = await req('GET', '/ratings', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const list = Array.isArray(r.json) ? r.json : r.json.content;
    return list.some(x => (x.feedback || '').includes('Verification pass')) || 'our rating not listed';
  });
  await check('GET /tickets/999999 -> 404 with error envelope', async () => {
    const r = await req('GET', '/tickets/999999', ctx.adminTok);
    const s = expectStatus(r, 404); if (s !== true) return s;
    return Boolean(r.json.timestamp && r.json.status === 404 && r.json.path) || 'envelope missing fields: ' + JSON.stringify(r.json).slice(0, 150);
  });
  await check('password change -> login with new works, old rejected', async () => {
    const email = `pwchange${uniq}@test.com`;
    const reg = await req('POST', '/auth/register', null, { fullName: 'Pw Change', email, password: 'Old@' + uniq, department: 'QA' });
    const tok = reg.json.token;
    const c = await req('PATCH', '/auth/password', tok, { currentPassword: 'Old@' + uniq, newPassword: 'New@' + uniq });
    const s = expectStatus(c, 200, 204); if (s !== true) return s;
    const l1 = await req('POST', '/auth/login', null, { email, password: 'New@' + uniq });
    const s2 = expectStatus(l1, 200); if (s2 !== true) return 'login with new pw: ' + s2;
    return expectStatus(await req('POST', '/auth/login', null, { email, password: 'Old@' + uniq }), 401);
  });

  await check('OpenAPI JSON /v3/api-docs -> 200', async () => {
    const res = await fetch('http://localhost:8081/v3/api-docs');
    return res.status === 200 || 'status ' + res.status;
  });
  await check('Swagger UI -> 200/302', async () => {
    const res = await fetch('http://localhost:8081/swagger-ui.html', { redirect: 'manual' });
    return [200, 302].includes(res.status) || 'status ' + res.status;
  });
}
