package com.itserve.config;

import com.itserve.entity.*;
import com.itserve.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Seeds realistic demo data on startup when the database is empty (H2 development profile). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "Password@123";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SlaRuleRepository slaRuleRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final RatingRepository ratingRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;

    private int ticketSequence = 0;
    private int commentOffset = 0;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      SlaRuleRepository slaRuleRepository, TicketRepository ticketRepository,
                      CommentRepository commentRepository, TicketHistoryRepository historyRepository,
                      NotificationRepository notificationRepository, RatingRepository ratingRepository,
                      KnowledgeArticleRepository articleRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.seed-demo-data}") boolean seedEnabled) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.slaRuleRepository = slaRuleRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
        this.ratingRepository = ratingRepository;
        this.articleRepository = articleRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }
        log.info("Seeding ITServe demo data...");

        seedCategories();
        seedSlaRules();

        User admin = seedUser("Priya Nair", "admin@itserve.com", Role.ADMIN, "IT Operations", null);
        User agent1 = seedUser("Amit Kumar", "agent1@itserve.com", Role.SUPPORT_AGENT, "IT Support", "Network,VPN,Security");
        User agent2 = seedUser("Sneha Patel", "agent2@itserve.com", Role.SUPPORT_AGENT, "IT Support", "Software,Application,Email");
        User agent3 = seedUser("Vikram Singh", "agent3@itserve.com", Role.SUPPORT_AGENT, "IT Support", "Hardware,Access Management,Other");
        User emp1 = seedUser("Rohan Mehta", "employee1@itserve.com", Role.EMPLOYEE, "Finance", null);
        User emp2 = seedUser("Ananya Iyer", "employee2@itserve.com", Role.EMPLOYEE, "Human Resources", null);
        User emp3 = seedUser("David Chen", "employee3@itserve.com", Role.EMPLOYEE, "Sales", null);

        Map<String, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        seedTickets(categories, List.of(agent1, agent2, agent3), List.of(emp1, emp2, emp3));
        seedKnowledgeArticles(admin, agent1, agent2);

        log.info("Demo data seeded. Login with admin@itserve.com / {} (or agent1@, employee1@)", DEMO_PASSWORD);
    }

    private void seedCategories() {
        Map<String, String> descriptions = Map.ofEntries(
                Map.entry("Hardware", "Laptops, monitors, printers and physical devices"),
                Map.entry("Software", "Installed applications and licensing"),
                Map.entry("Network", "Wi-Fi, LAN and connectivity issues"),
                Map.entry("Email", "Outlook, mail delivery and mailbox issues"),
                Map.entry("VPN", "Corporate VPN access and client issues"),
                Map.entry("Access Management", "Permissions, accounts and password resets"),
                Map.entry("Application", "Line-of-business applications (Salesforce, ERP, etc.)"),
                Map.entry("Security", "Antivirus, phishing and security incidents"),
                Map.entry("Other", "Anything that does not fit another category"));
        descriptions.forEach((name, description) -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            categoryRepository.save(category);
        });
    }

    private void seedSlaRules() {
        slaRuleRepository.save(new SlaRule(Priority.CRITICAL, 2));
        slaRuleRepository.save(new SlaRule(Priority.HIGH, 4));
        slaRuleRepository.save(new SlaRule(Priority.MEDIUM, 8));
        slaRuleRepository.save(new SlaRule(Priority.LOW, 24));
    }

    private User seedUser(String fullName, String email, Role role, String department, String skills) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setDepartment(department);
        user.setSkills(skills);
        return userRepository.save(user);
    }

    private void seedTickets(Map<String, Category> categories, List<User> agents, List<User> employees) {
        User agent1 = agents.get(0), agent2 = agents.get(1), agent3 = agents.get(2);
        User emp1 = employees.get(0), emp2 = employees.get(1), emp3 = employees.get(2);
        Category vpn = categories.get("VPN"), network = categories.get("Network"), email = categories.get("Email"),
                software = categories.get("Software"), hardware = categories.get("Hardware"),
                access = categories.get("Access Management"), application = categories.get("Application"),
                security = categories.get("Security");

        seedTicket("VPN connection failing with error 800",
                "Unable to connect to the corporate VPN since this morning. Cisco AnyConnect shows error 800: Unable to establish VPN session. Tried restarting the client and flushing DNS.",
                vpn, Priority.HIGH, emp1, agent1, TicketStatus.IN_PROGRESS, 5, null, null);
        seedTicket("Office Wi-Fi down on 3rd floor",
                "Multiple users on the 3rd floor cannot connect to the office Wi-Fi. The access points show no SSID broadcast. Wired connections still work.",
                network, Priority.CRITICAL, emp2, agent1, TicketStatus.ESCALATED, 3, null, null);
        seedTicket("Outlook repeatedly prompting for password",
                "Outlook keeps asking for my password every few minutes even after re-entering it. Mail sync stopped working this afternoon.",
                email, Priority.MEDIUM, emp3, agent2, TicketStatus.RESOLVED, 30, 6.0,
                "Cached credentials were corrupted. Cleared Windows credentials for office365 and re-authenticated. Sync restored.");
        seedTicket("Request Figma license installation",
                "Our design team needs Figma desktop installed on two machines. License approved by the marketing head (approval email attached).",
                software, Priority.LOW, emp1, agent2, TicketStatus.ASSIGNED, 10, null, null);
        seedTicket("Laptop not booting - blue screen",
                "My Dell Latitude shows a blue screen (INACCESSIBLE_BOOT_DEVICE) during startup after last night's Windows update. Cannot reach the desktop at all.",
                hardware, Priority.HIGH, emp2, agent3, TicketStatus.IN_PROGRESS, 8, null, null);
        seedTicket("Password reset for HR portal",
                "Locked out of the HR self-service portal after too many failed attempts. Need a reset before the payroll cutoff.",
                access, Priority.MEDIUM, emp3, agent3, TicketStatus.CLOSED, 50, 3.0,
                "Identity verified with manager approval. Password reset through AD and portal sync confirmed.");
        seedTicket("VPN drops every 10 minutes",
                "The corporate VPN session disconnects roughly every 10 minutes. This makes remote access to the file share unusable.",
                vpn, Priority.HIGH, emp3, agent1, TicketStatus.OPEN, 1, null, null);
        seedTicket("2nd floor printer offline",
                "The shared printer near the 2nd floor pantry shows offline for everyone in the team. Restarting the printer did not help.",
                hardware, Priority.LOW, emp1, agent3, TicketStatus.RESOLVED, 26, 5.0,
                "Print spooler service on the print server was hung. Restarted the service and re-registered the queue.");
        seedTicket("Cannot access shared drive S:",
                "The S: drive mapping disappeared after the weekend. Re-mapping asks for credentials that used to work.",
                network, Priority.MEDIUM, emp2, agent1, TicketStatus.WAITING_FOR_USER, 12, null, null);
        seedTicket("Zoom crashes during screen sharing",
                "Zoom 6.x crashes immediately when I start screen sharing in any meeting. Reinstalling did not fix it.",
                application, Priority.MEDIUM, emp1, agent2, TicketStatus.OPEN, 6, null, null);
        seedTicket("New joiner email account required",
                "New joiner in Sales (joining Monday) needs an email account, distribution list membership and a shared mailbox delegate.",
                email, Priority.HIGH, emp3, agent2, TicketStatus.RESOLVED, 55, 2.0,
                "Mailbox created per HR request, added to sales-team@ distribution list and delegated the regional shared mailbox.");
        seedTicket("Suspicious phishing email reported",
                "Received an email claiming to be from IT asking to verify credentials via a link. Sender domain looks like a typo-squat of our domain. Reporting for investigation.",
                security, Priority.CRITICAL, emp2, agent1, TicketStatus.IN_PROGRESS, 1.5, null, null);
        seedTicket("VPN client update failing with error 0x8007",
                "AnyConnect auto-update fails with error 0x8007 and rolls back. Manual installer also fails at 80%.",
                vpn, Priority.HIGH, emp1, agent1, TicketStatus.OPEN, 2, null, null);
        seedTicket("Admin rights needed for developer tools",
                "Need local admin rights to install Node.js and Docker for the new project. Manager approval attached.",
                access, Priority.LOW, emp2, agent3, TicketStatus.CLOSED, 80, 20.0,
                "Added to local admins group via endpoint manager per approved request.");
        seedTicket("Excel crashes when opening large files",
                "Excel 365 freezes and crashes whenever I open workbooks above ~50MB. Smaller files work fine. Happens on my laptop only.",
                software, Priority.MEDIUM, emp3, agent2, TicketStatus.IN_PROGRESS, 20, null, null);
        seedTicket("VPN latency to Mumbai office",
                "RDP sessions through VPN to the Mumbai office are extremely laggy since Tuesday. Direct internet is fine.",
                vpn, Priority.MEDIUM, emp1, agent1, TicketStatus.REOPENED, 40, null, null);
        seedTicket("Monitor flickering with docking station",
                "External monitor connected via the WD19 dock flickers every few seconds. Replacing the HDMI cable did not help.",
                hardware, Priority.LOW, emp3, agent3, TicketStatus.ASSIGNED, 15, null, null);
        seedTicket("Cannot login to Salesforce",
                "Getting 'invalid session' immediately after login on Salesforce. Other users in the team are unaffected.",
                application, Priority.HIGH, emp2, agent2, TicketStatus.IN_PROGRESS, 4, null, null);
        seedTicket("VPN setup on new laptop",
                "Received my replacement laptop. Need the corporate VPN profile and certificate installed.",
                vpn, Priority.LOW, emp1, agent1, TicketStatus.RESOLVED, 70, 6.0,
                "Deployed the VPN profile via Intune and installed the machine certificate. Verified connection to HQ network.");
        seedTicket("Email attachments blocked by gateway",
                "Legitimate ZIP attachments from a known vendor are being quarantined by the email gateway since the new policy update.",
                email, Priority.MEDIUM, emp3, agent2, TicketStatus.OPEN, 9, null, null);
        seedTicket("Cisco AnyConnect certificate warning",
                "AnyConnect shows 'server certificate not trusted' when connecting to vpn.corp.local. Connection is refused after the warning.",
                vpn, Priority.HIGH, emp2, agent1, TicketStatus.OPEN, 3, null, null);
        seedTicket("File server latency during business hours",
                "Saving files to the finance file share takes minutes during business hours. Performance was fine last month.",
                network, Priority.CRITICAL, emp1, agent1, TicketStatus.RESOLVED, 100, 1.5,
                "A runaway backup job was saturating the storage network. Rescheduled backups to off-hours; latency back to normal.");
        seedTicket("Antivirus blocking internal tool",
                "Endpoint antivirus quarantined our internal invoice-matching tool after the latest signature update.",
                security, Priority.HIGH, emp3, agent1, TicketStatus.IN_PROGRESS, 28, null, null);
        seedTicket("Keyboard keys not responding",
                "Several keys (Q, W, E, R) on my laptop keyboard stopped responding. External keyboard works fine.",
                hardware, Priority.MEDIUM, emp2, null, TicketStatus.OPEN, 30, null, null);
        seedTicket("Teams meeting audio issues",
                "Colleagues cannot hear me in Teams meetings. Microphone works in other apps and in the Teams test call.",
                application, Priority.LOW, emp1, agent2, TicketStatus.CLOSED, 120, 10.0,
                "Windows privacy settings blocked desktop apps from accessing the microphone after the update. Restored permission.");
        seedTicket("ERP access request for finance team",
                "Three new analysts in Finance need read access to the ERP reporting module. Request approved by the CFO's office.",
                access, Priority.MEDIUM, emp1, null, TicketStatus.OPEN, 27, null, null);

        seedComments();
        seedRatings();
    }

    private Ticket seedTicket(String subject, String description, Category category, Priority priority,
                              User creator, User agent, TicketStatus status, double createdHoursAgo,
                              Double resolvedHoursAfterCreate, String resolutionNote) {
        Instant now = Instant.now();
        Instant createdAt = now.minus((long) (createdHoursAgo * 60), ChronoUnit.MINUTES);
        int slaHours = switch (priority) {
            case CRITICAL -> 2;
            case HIGH -> 4;
            case MEDIUM -> 8;
            case LOW -> 24;
        };

        Ticket ticket = new Ticket();
        ticket.setTicketNumber(String.format("INC-%d-%06d",
                createdAt.atZone(java.time.ZoneId.systemDefault()).getYear(), ++ticketSequence));
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setCreatedBy(creator);
        ticket.setStatus(status);
        ticket.setLocation("HQ - Bengaluru");
        ticket.setPreferredContact("EMAIL");
        ticket.setCreatedAt(createdAt);
        ticket.setUpdatedAt(createdAt);
        ticket.setSlaDeadline(createdAt.plus(slaHours, ChronoUnit.HOURS));

        if (agent != null) {
            ticket.setAssignedAgent(agent);
        }
        if (status != TicketStatus.OPEN && status != TicketStatus.REOPENED) {
            ticket.setFirstResponseAt(createdAt.plus(45, ChronoUnit.MINUTES));
        }
        if (resolvedHoursAfterCreate != null && status != TicketStatus.REOPENED) {
            ticket.setResolvedAt(createdAt.plus((long) (resolvedHoursAfterCreate * 60), ChronoUnit.MINUTES));
            ticket.setResolutionNote(resolutionNote);
        }
        ticket.setSlaBreachedNotified(ticket.getSlaDeadline().isBefore(now)
                && status != TicketStatus.RESOLVED && status != TicketStatus.CLOSED);
        ticketRepository.save(ticket);

        historyRepository.save(history(ticket, creator, createdAt, "CREATED",
                "Ticket created with priority " + priority));
        if (agent != null) {
            historyRepository.save(history(ticket, adminUserOf(ticket), createdAt.plus(20, ChronoUnit.MINUTES),
                    "ASSIGNED", "Assigned to " + agent.getFullName()));
            notification(agent, ticket, NotificationType.TICKET_ASSIGNED,
                    "Ticket " + ticket.getTicketNumber() + " assigned to you", createdAt.plus(20, ChronoUnit.MINUTES));
            if (status != TicketStatus.OPEN) {
                historyRepository.save(history(ticket, agent, createdAt.plus(45, ChronoUnit.MINUTES),
                        "STATUS_CHANGED", "ASSIGNED -> IN_PROGRESS"));
            }
        }
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            historyRepository.save(history(ticket, agent, ticket.getResolvedAt(), "STATUS_CHANGED",
                    "IN_PROGRESS -> RESOLVED (" + resolutionNote + ")"));
            notification(creator, ticket, NotificationType.TICKET_RESOLVED,
                    "Your ticket " + ticket.getTicketNumber() + " was resolved", ticket.getResolvedAt());
        }
        if (status == TicketStatus.ESCALATED) {
            historyRepository.save(history(ticket, agent, createdAt.plus(90, ChronoUnit.MINUTES), "ESCALATED",
                    "Escalated: multiple users affected, network team engaged"));
        }
        if (ticket.isSlaBreachedNotified()) {
            historyRepository.save(history(ticket, creator, ticket.getSlaDeadline(), "SLA_BREACHED",
                    "SLA deadline passed without resolution"));
            notification(agent, ticket, NotificationType.SLA_BREACHED,
                    "Ticket " + ticket.getTicketNumber() + " SLA breached", ticket.getSlaDeadline());
        }
        notification(creator, ticket, NotificationType.TICKET_CREATED,
                "Your ticket " + ticket.getTicketNumber() + " was created", createdAt);
        return ticket;
    }

    private User adminUserOf(Ticket ticket) {
        return userRepository.findByEmail("admin@itserve.com").orElse(ticket.getCreatedBy());
    }

    private TicketHistory history(Ticket ticket, User by, Instant at, String action, String detail) {
        TicketHistory history = new TicketHistory(ticket, by, action, detail);
        history.setCreatedAt(at);
        return history;
    }

    private void notification(User recipient, Ticket ticket, NotificationType type, String message, Instant at) {
        if (recipient == null) {
            return;
        }
        Notification notification = new Notification(recipient, ticket, type, message);
        notification.setCreatedAt(at);
        // Keep only recent notifications unread so the bell badge looks realistic.
        notification.setRead(at.isBefore(Instant.now().minus(24, ChronoUnit.HOURS)));
        notificationRepository.save(notification);
    }

    private void seedComments() {
        Map<String, Ticket> byNumberSubject = ticketsBySubject();
        commentOffset = 0;
        addComment(byNumberSubject.get("VPN connection failing with error 800"),
                "Amit Kumar", "Please restart the VPN client and try connecting to the vpn-backup.corp.local gateway meanwhile.");
        addComment(byNumberSubject.get("VPN connection failing with error 800"),
                "Rohan Mehta", "Backup gateway works but is very slow. Still cannot reach the primary.");
        addComment(byNumberSubject.get("VPN connection failing with error 800"),
                "Amit Kumar", "Primary concentrator has a faulty certificate; network team is replacing it. ETA 2 hours.");
        addComment(byNumberSubject.get("Outlook repeatedly prompting for password"),
                "Sneha Patel", "Cleared the cached Office credentials and re-authenticated. Please confirm mail sync is working.");
        addComment(byNumberSubject.get("Outlook repeatedly prompting for password"),
                "David Chen", "Working now. Thank you for the quick fix!");
        addComment(byNumberSubject.get("Laptop not booting - blue screen"),
                "Vikram Singh", "Booted into recovery and rolled back the problematic update KB5065xxx. Running disk check now.");
        addComment(byNumberSubject.get("Office Wi-Fi down on 3rd floor"),
                "Amit Kumar", "Confirmed 3rd floor access points lost power - PoE switch reboot did not help. Escalating to facilities for the switch stack.");
        addComment(byNumberSubject.get("Suspicious phishing email reported"),
                "Amit Kumar", "Thanks for reporting. Domain is a known typo-squat; blocked at the gateway and purged the message from all mailboxes.");
        addComment(byNumberSubject.get("Cannot access shared drive S:"),
                "Amit Kumar", "Your AD password expired over the weekend which broke the saved mapping. Please reset via the portal and try again - marking as waiting for you.");
        addComment(byNumberSubject.get("VPN drops every 10 minutes"),
                "Rohan Mehta", "Issue is still occurring even after rebooting my router at home.");
    }

    private Map<String, Ticket> ticketsBySubject() {
        return ticketRepository.findAll().stream()
                .collect(Collectors.toMap(Ticket::getSubject, Function.identity(), (a, b) -> a));
    }

    private void seedRatings() {
        Map<String, Ticket> bySubject = ticketsBySubject();
        addRating(bySubject.get("Outlook repeatedly prompting for password"), 5, "Very quick resolution, Outlook works perfectly now.");
        addRating(bySubject.get("Password reset for HR portal"), 4, "Reset was done well before the payroll cutoff.");
        addRating(bySubject.get("2nd floor printer offline"), 5, "Printer back online within the day.");
        addRating(bySubject.get("New joiner email account required"), 5, "Everything was ready before the joiner's first day.");
        addRating(bySubject.get("Admin rights needed for developer tools"), 3, "Access granted, though it took almost a day.");
        addRating(bySubject.get("VPN setup on new laptop"), 4, "VPN works well, setup took a few hours.");
        addRating(bySubject.get("File server latency during business hours"), 5, "Root cause found fast. Excellent response for a critical issue.");
        addRating(bySubject.get("Teams meeting audio issues"), 4, "Fixed after checking privacy settings.");
    }

    private void addRating(Ticket ticket, int stars, String feedback) {
        if (ticket == null || ticket.getResolvedAt() == null) {
            return;
        }
        Rating rating = new Rating();
        rating.setTicket(ticket);
        rating.setCreatedBy(ticket.getCreatedBy());
        rating.setStars(stars);
        rating.setFeedback(feedback);
        rating.setCreatedAt(ticket.getResolvedAt().plus(2, ChronoUnit.HOURS));
        ratingRepository.save(rating);
        historyRepository.save(history(ticket, ticket.getCreatedBy(), rating.getCreatedAt(), "RATED",
                "Rated " + stars + "/5 stars"));
    }

    private void addComment(Ticket ticket, String authorEmailName, String message) {
        if (ticket == null) {
            return;
        }
        User author = userRepository.findAll().stream()
                .filter(u -> u.getFullName().equals(authorEmailName))
                .findFirst().orElse(null);
        if (author == null) {
            return;
        }
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setMessage(message);
        comment.setCreatedAt(ticket.getCreatedAt().plus(60L + (++commentOffset * 25L), ChronoUnit.MINUTES));
        commentRepository.save(comment);
        historyRepository.save(history(ticket, author, comment.getCreatedAt(), "COMMENT_ADDED",
                "Comment added by " + author.getFullName()));
    }

    private void seedKnowledgeArticles(User admin, User agent1, User agent2) {
        seedArticle("How to reset your corporate password", "Access Management", admin,
                """
                1. Go to the self-service password portal at password.corp.local.
                2. Enter your employee ID and click "Forgot password".
                3. Verify your identity using the authenticator app or the SMS code sent to your registered phone.
                4. Choose a new password that meets the complexity requirements (12+ characters, mixed case, number, symbol).
                5. Sign out of all devices, then sign back in with the new password.

                If self-service fails, raise a ticket under "Access Management" - the IT team can reset it after verifying you with your manager.
                """);
        seedArticle("How to connect to the corporate VPN", "VPN", agent1,
                """
                1. Install Cisco AnyConnect from software.corp.local/vpn (or the IT Self Service app).
                2. Launch AnyConnect and enter vpn.corp.local as the server.
                3. Authenticate with your corporate credentials plus the MFA prompt on your phone.
                4. Once connected, the padlock icon in the system tray turns green.

                Common issues:
                - Error 800: usually a local firewall or ISP block. Try the backup gateway vpn-backup.corp.local.
                - Certificate warning: your machine certificate expired - raise a VPN ticket and we will re-enroll the device.
                """);
        seedArticle("How to configure Outlook on a new machine", "Email", agent2,
                """
                1. Open Outlook and add your corporate email address.
                2. Choose "Office 365 / Exchange" when asked about the account type - do not use IMAP.
                3. Sign in with your corporate credentials when the modern authentication window appears.
                4. Let Outlook complete the initial mailbox sync before changing cache settings.

                If Outlook keeps prompting for your password, clear saved credentials:
                Control Panel > Credential Manager > Windows Credentials > remove entries starting with "MicrosoftOffice16".
                """);
        seedArticle("How to connect to office Wi-Fi", "Network", agent1,
                """
                1. Select the SSID "CorpNet-Secure" from the Wi-Fi list.
                2. When prompted, enter your corporate username (email) and password.
                3. Accept the certificate prompt the first time you connect.
                4. Guest devices should use "CorpNet-Guest" with the daily passcode posted at reception.

                If CorpNet-Secure does not appear on an entire floor, the access points may be offline - raise a CRITICAL network ticket so facilities can be engaged.
                """);
        seedArticle("How to request access to an application", "Access Management", admin,
                """
                1. Get approval from your manager or the application owner (email is fine).
                2. Raise a ticket in ITServe under the "Access Management" category.
                3. In the description include: application name, access level (read/write/admin), business justification, and the approver's name.
                4. IT provisions access, usually within one business day for standard applications.

                Note: admin-level access and finance systems require a second approval from the department head.
                """);
        seedArticle("What to do if you receive a phishing email", "Security", agent1,
                """
                1. Do NOT click any links or open attachments.
                2. Use the "Report Phishing" button in Outlook - this sends the full message to the security team.
                3. If you already clicked the link or entered credentials, immediately reset your password and raise a CRITICAL Security ticket.
                4. The security team will block the sender domain and purge the message from all mailboxes.

                Red flags: urgency ("within 24 hours"), lookalike sender domains, generic greetings, unexpected attachments.
                """);
    }

    private void seedArticle(String title, String category, User author, String content) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(title);
        article.setCategory(category);
        article.setContent(content.stripIndent().trim());
        article.setAuthor(author);
        article.setHelpfulYes(8 + title.length() % 15);
        article.setHelpfulNo(title.length() % 3);
        article.setCreatedAt(Instant.now().minus(30 + article.getHelpfulNo(), ChronoUnit.DAYS));
        articleRepository.save(article);
    }
}
