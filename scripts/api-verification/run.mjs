import { report } from './lib.mjs';
import { runPart1 } from './part1-auth-users-sla.mjs';
import { runPart2 } from './part2-tickets.mjs';
import { runPart3 } from './part3-dashboards-kb-misc.mjs';

await runPart1();
await runPart2();
await runPart3();
process.exit(report() ? 1 : 0);
