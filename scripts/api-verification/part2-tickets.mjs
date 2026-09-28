import { req, check, expectStatus, PW, uniq, ctx } from './lib.mjs';

export async function runPart2() {
  await check('GET /tickets (admin) -> paged, >=20 tickets, INC-YYYY-NNNNNN format', async () => {
    const r = await req('GET', '/tickets?size=100', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const total = r.json.totalElements ?? r.json.content?.length;
    if (total < 20) return 'only ' + total + ' tickets';
    const bad = r.json.content.filter(t => !/^INC-\d{4}-\d{6}$/.test(t.ticketNumber));
    return bad.length === 0 || 'bad ticket numbers: ' + bad.map(t => t.ticketNumber).join(',');
  });
  await check('GET /tickets as employee -> only own tickets', async () => {
    const r = await req('GET', '/tickets?size=100', ctx.empTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const foreign = r.json.content.filter(t => t.createdBy?.id !== ctx.emp1Id);
    return foreign.length === 0 || foreign.length + ' foreign tickets visible';
  });
  await check('pagination ?page=0&size=5 -> <=5 items + paging metadata', async () => {
    const r = await req('GET', '/tickets?page=0&size=5', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    if (r.json.content.length > 5) return 'returned ' + r.json.content.length;
    return r.json.totalPages > 1 || 'totalPages not > 1';
  });
  await check('filter ?status=OPEN -> all OPEN', async () => {
    const r = await req('GET', '/tickets?status=OPEN&size=100', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    return r.json.content.every(t => t.status === 'OPEN') || 'mixed statuses';
  });
  await check('filter ?priority=HIGH -> all HIGH', async () => {
    const r = await req('GET', '/tickets?priority=HIGH&size=100', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    return r.json.content.every(t => t.priority === 'HIGH') || 'mixed priorities';
  });
  await check('search ?search=vpn -> matching rows only', async () => {
    const r = await req('GET', '/tickets?search=vpn&size=100', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    if (r.json.content.length === 0) return 'no results for "vpn"';
    const bad = r.json.content.filter(t => !((t.subject + ' ' + t.ticketNumber).toLowerCase().includes('vpn')));
    return bad.length === 0 || bad.length + ' non-matching rows';
  });
  await check('filter ?sla=breached -> rows flagged BREACHED', async () => {
    const r = await req('GET', '/tickets?sla=breached&size=100', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    return r.json.content.every(t => (t.slaStatus || '').toUpperCase().includes('BREACH')) || 'rows not breached: ' + JSON.stringify(r.json.content.map(t => t.slaStatus));
  });
  await check('filter ?assignedTo=unassigned -> no assignee', async () => {
    const r = await req('GET', '/tickets?assignedTo=unassigned&size=100', ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    return r.json.content.every(t => !t.assignedAgent) || 'assigned tickets returned';
  });

  // ---- full lifecycle on a fresh ticket ----
  await check('employee create ticket -> OPEN, INC number, SLA deadline = +4h (HIGH)', async () => {
    const netCat = ctx.categories.find(c => /network/i.test(c.name)) || ctx.categories[0];
    const r = await req('POST', '/tickets', ctx.empTok, { subject: 'Verify feature test ' + uniq, description: 'Automated end-to-end verification ticket', categoryId: netCat.id, priority: 'HIGH', location: 'Test lab' });
    const s = expectStatus(r, 200, 201); if (s !== true) return s;
    ctx.newTicket = r.json.ticket ?? r.json;
    const t = ctx.newTicket;
    if (t.status !== 'OPEN') return 'status ' + t.status;
    if (!/^INC-\d{4}-\d{6}$/.test(t.ticketNumber)) return 'bad number ' + t.ticketNumber;
    if (!t.slaDeadline) return 'no slaDeadline';
    const hrs = (new Date(t.slaDeadline) - new Date(t.createdAt)) / 3600000;
    return Math.abs(hrs - 4) < 0.15 || 'sla deadline ' + hrs.toFixed(2) + 'h after creation';
  });
  await check('employee resolve own new ticket -> blocked', async () =>
    expectStatus(await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, ctx.empTok, { status: 'RESOLVED', note: 'hack' }), 400, 403, 409, 422));
  await check('agent self-assign -> 403 (admin only)', async () =>
    expectStatus(await req('PATCH', `/tickets/${ctx.newTicket.id}/assign`, ctx.agentTok, { agentId: ctx.agent1Id }), 403));
  await check('suggested-agents -> ranked list with skillMatch + fullName', async () => {
    const r = await req('GET', `/tickets/${ctx.newTicket.id}/suggested-agents`, ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    const list = Array.isArray(r.json) ? r.json : r.json.content;
    if (!list.length) return 'empty suggestions';
    return (list[0].skillMatch !== undefined && list[0].fullName !== undefined) || 'missing fields: ' + JSON.stringify(list[0]);
  });
  await check('admin assign agent1 -> ASSIGNED', async () => {
    const r = await req('PATCH', `/tickets/${ctx.newTicket.id}/assign`, ctx.adminTok, { agentId: ctx.agent1Id });
    const s = expectStatus(r, 200); if (s !== true) return s;
    const t = r.json.ticket ?? r.json;
    return t.status === 'ASSIGNED' || 'status ' + t.status;
  });
  await check('agent1 IN_PROGRESS -> 200', async () => {
    const r = await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, ctx.agentTok, { status: 'IN_PROGRESS' });
    const s = expectStatus(r, 200); if (s !== true) return s;
    const t = r.json.ticket ?? r.json;
    return t.status === 'IN_PROGRESS' || 'status ' + t.status;
  });
  await check('agent resolve without note -> 400/422', async () =>
    expectStatus(await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, ctx.agentTok, { status: 'RESOLVED' }), 400, 422));
  await check('invalid transition IN_PROGRESS -> ASSIGNED blocked', async () =>
    expectStatus(await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, ctx.agentTok, { status: 'ASSIGNED' }), 400, 409, 422));
  await check('employee comment -> saved and listed', async () => {
    const r = await req('POST', `/tickets/${ctx.newTicket.id}/comments`, ctx.empTok, { message: 'Verification comment ' + uniq });
    const s = expectStatus(r, 200, 201); if (s !== true) return s;
    const l = await req('GET', `/tickets/${ctx.newTicket.id}/comments`, ctx.empTok);
    const found = (Array.isArray(l.json) ? l.json : l.json.content).some(c => c.message.includes(String(uniq)));
    return found || 'comment not found in list';
  });
  await check('rate before RESOLVED -> blocked', async () =>
    expectStatus(await req('POST', `/tickets/${ctx.newTicket.id}/rating`, ctx.empTok, { stars: 5 }), 400, 409, 422));
  await check('agent RESOLVED with note -> 200', async () => {
    const r = await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, ctx.agentTok, { status: 'RESOLVED', note: 'Fixed during verification run' });
    const s = expectStatus(r, 200); if (s !== true) return s;
    const t = r.json.ticket ?? r.json;
    return t.status === 'RESOLVED' || 'status ' + t.status;
  });
  await check('non-owner employee cannot close -> 403', async () => {
    const l = await req('POST', '/auth/login', null, { email: 'employee2@itserve.com', password: PW });
    return expectStatus(await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, l.json.token, { status: 'CLOSED' }), 403);
  });
  await check('owner rates 5 stars + feedback -> stored, GET returns it', async () => {
    const r = await req('POST', `/tickets/${ctx.newTicket.id}/rating`, ctx.empTok, { stars: 5, feedback: 'Verification pass' });
    const s = expectStatus(r, 200, 201); if (s !== true) return s;
    const g = await req('GET', `/tickets/${ctx.newTicket.id}/rating`, ctx.empTok);
    const s2 = expectStatus(g, 200); if (s2 !== true) return s2;
    return g.json.stars === 5 || 'stars ' + g.json.stars;
  });
  await check('duplicate rating -> blocked', async () =>
    expectStatus(await req('POST', `/tickets/${ctx.newTicket.id}/rating`, ctx.empTok, { stars: 3 }), 400, 409, 422));
  await check('owner closes RESOLVED ticket -> CLOSED', async () => {
    const r = await req('PATCH', `/tickets/${ctx.newTicket.id}/status`, ctx.empTok, { status: 'CLOSED' });
    const s = expectStatus(r, 200); if (s !== true) return s;
    const t = r.json.ticket ?? r.json;
    return t.status === 'CLOSED' || 'status ' + t.status;
  });
  await check('comment on CLOSED ticket -> blocked', async () =>
    expectStatus(await req('POST', `/tickets/${ctx.newTicket.id}/comments`, ctx.empTok, { message: 'late' }), 400, 409, 422));
  await check('ticket detail -> {ticket, comments, history} with lifecycle entries', async () => {
    const r = await req('GET', `/tickets/${ctx.newTicket.id}`, ctx.adminTok);
    const s = expectStatus(r, 200); if (s !== true) return s;
    if (!r.json.ticket || !Array.isArray(r.json.comments) || !Array.isArray(r.json.history)) return 'shape wrong: ' + Object.keys(r.json);
    return r.json.history.length >= 4 || 'only ' + r.json.history.length + ' history entries';
  });
  await check('unrelated employee GET ticket detail -> 403/404', async () => {
    const l = await req('POST', '/auth/login', null, { email: 'employee3@itserve.com', password: PW });
    return expectStatus(await req('GET', `/tickets/${ctx.newTicket.id}`, l.json.token), 403, 404);
  });

  // ---- escalation + priority on a seeded ticket ----
  await check('escalate: admin assigns, agent escalates -> ESCALATED', async () => {
    const open = await req('GET', '/tickets?status=OPEN&size=1', ctx.adminTok);
    const t = open.json.content[0];
    if (!t) return 'no OPEN ticket to escalate';
    ctx.escTicket = t;
    const a = await req('PATCH', `/tickets/${t.id}/assign`, ctx.adminTok, { agentId: ctx.agent1Id });
    const s1 = expectStatus(a, 200); if (s1 !== true) return s1;
    const e = await req('PATCH', `/tickets/${t.id}/escalate`, ctx.agentTok, {});
    const s2 = expectStatus(e, 200); if (s2 !== true) return s2;
    const et = e.json.ticket ?? e.json;
    return et.status === 'ESCALATED' || 'status ' + et.status;
  });
  await check('employee escalate -> 403', async () =>
    expectStatus(await req('PATCH', `/tickets/${ctx.escTicket.id}/escalate`, ctx.empTok, {}), 403));
  await check('admin change priority -> CRITICAL applied', async () => {
    const r = await req('PATCH', `/tickets/${ctx.escTicket.id}/priority`, ctx.adminTok, { priority: 'CRITICAL' });
    const s = expectStatus(r, 200); if (s !== true) return s;
    const t = r.json.ticket ?? r.json;
    return t.priority === 'CRITICAL' || 'priority ' + t.priority;
  });
  await check('agent change priority -> 403', async () =>
    expectStatus(await req('PATCH', `/tickets/${ctx.escTicket.id}/priority`, ctx.agentTok, { priority: 'LOW' }), 403));

  // ---- attachments ----
  await check('upload + download attachment', async () => {
    const open = await req('GET', '/tickets?status=OPEN&size=1', ctx.adminTok);
    const t = open.json.content[0];
    if (!t) return 'no OPEN ticket';
    const fd = new FormData();
    fd.append('file', new Blob(['verification file content'], { type: 'text/plain' }), 'verify.txt');
    const up = await req('POST', `/tickets/${t.id}/attachments`, ctx.adminTok, fd, true);
    const s = expectStatus(up, 200, 201); if (s !== true) return s;
    const attId = up.json.id ?? up.json.attachment?.id;
    if (!attId) return 'no attachment id: ' + JSON.stringify(up.json).slice(0, 150);
    const res = await fetch(`http://localhost:8081/api/tickets/attachments/${attId}`, { headers: { Authorization: 'Bearer ' + ctx.adminTok } });
    if (res.status !== 200) return 'download status ' + res.status;
    const text = await res.text();
    return text === 'verification file content' || 'content mismatch: ' + text.slice(0, 80);
  });
}
