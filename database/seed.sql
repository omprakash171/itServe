-- ITServe — reference seed data for MySQL.
--
-- NOTE: when the backend starts with `app.seed-demo-data=true` (the default)
-- it seeds a much richer demo dataset (26+ tickets, comments, ratings,
-- notifications, knowledge articles) through DataSeeder. This script covers
-- the baseline reference data: categories, SLA rules and the demo accounts.
--
-- All demo accounts use the password: Password@123
-- (BCrypt hash below, strength 10).

USE itserve;

INSERT INTO users (full_name, email, password, role, department, skills, active, created_at) VALUES
('Priya Nair',    'admin@itserve.com',     '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'ADMIN',         'IT Operations', NULL,                              1, NOW(6)),
('Amit Kumar',    'agent1@itserve.com',    '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'SUPPORT_AGENT', 'IT Support',    'Network,VPN,Security',            1, NOW(6)),
('Sneha Patel',   'agent2@itserve.com',    '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'SUPPORT_AGENT', 'IT Support',    'Software,Application,Email',      1, NOW(6)),
('Vikram Singh',  'agent3@itserve.com',    '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'SUPPORT_AGENT', 'IT Support',    'Hardware,Access Management,Other',1, NOW(6)),
('Rohan Mehta',   'employee1@itserve.com', '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'EMPLOYEE',      'Finance',       NULL,                              1, NOW(6)),
('Ananya Iyer',   'employee2@itserve.com', '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'EMPLOYEE',      'Human Resources', NULL,                            1, NOW(6)),
('David Chen',    'employee3@itserve.com', '$2a$10$c/Ypem.B3v3cDLPGHNKi6umrs/EEVxgDHI/5BmcMkOOcHWdZ7i5Fy', 'EMPLOYEE',      'Sales',         NULL,                              1, NOW(6));

INSERT INTO categories (name, description) VALUES
('Hardware',          'Laptops, monitors, printers and physical devices'),
('Software',          'Installed applications and licensing'),
('Network',           'Wi-Fi, LAN and connectivity issues'),
('Email',             'Outlook, mail delivery and mailbox issues'),
('VPN',               'Corporate VPN access and client issues'),
('Access Management', 'Permissions, accounts and password resets'),
('Application',       'Line-of-business applications (Salesforce, ERP, etc.)'),
('Security',          'Antivirus, phishing and security incidents'),
('Other',             'Anything that does not fit another category');

INSERT INTO sla_rules (priority, resolution_hours) VALUES
('CRITICAL', 2),
('HIGH', 4),
('MEDIUM', 8),
('LOW', 24);

-- Example ticket (created by employee1, assigned to agent1, HIGH -> 4h SLA)
INSERT INTO tickets (ticket_number, subject, description, status, priority, category_id,
                     created_by_id, assigned_agent_id, location, preferred_contact,
                     sla_deadline, first_response_at, created_at, updated_at)
SELECT 'INC-2026-000001',
       'VPN connection failing with error 800',
       'Unable to connect to the corporate VPN since this morning. Cisco AnyConnect shows error 800.',
       'ASSIGNED', 'HIGH', c.id, u_emp.id, u_agent.id, 'Remote', 'EMAIL',
       DATE_ADD(NOW(6), INTERVAL 4 HOUR), NOW(6), NOW(6), NOW(6)
FROM categories c, users u_emp, users u_agent
WHERE c.name = 'VPN' AND u_emp.email = 'employee1@itserve.com' AND u_agent.email = 'agent1@itserve.com';

INSERT INTO ticket_history (ticket_id, performed_by_id, action, detail, created_at)
SELECT t.id, u.id, 'CREATED', 'Ticket created with priority HIGH', NOW(6)
FROM tickets t, users u
WHERE t.ticket_number = 'INC-2026-000001' AND u.email = 'employee1@itserve.com';
