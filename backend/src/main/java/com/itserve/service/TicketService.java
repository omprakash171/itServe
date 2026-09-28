package com.itserve.service;

import com.itserve.dto.*;
import com.itserve.entity.*;
import com.itserve.exception.ForbiddenActionException;
import com.itserve.exception.InvalidTicketStatusException;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final List<String> SORTABLE = List.of(
            "createdAt", "updatedAt", "priority", "status", "slaDeadline", "ticketNumber", "subject");

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final RatingRepository ratingRepository;
    private final AttachmentRepository attachmentRepository;
    private final SlaService slaService;
    private final NotificationService notificationService;

    public TicketService(TicketRepository ticketRepository, CategoryRepository categoryRepository,
                         UserRepository userRepository, CommentRepository commentRepository,
                         TicketHistoryRepository historyRepository, NotificationRepository notificationRepository,
                         RatingRepository ratingRepository, AttachmentRepository attachmentRepository,
                         SlaService slaService, NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
        this.ratingRepository = ratingRepository;
        this.attachmentRepository = attachmentRepository;
        this.slaService = slaService;
        this.notificationService = notificationService;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, User creator) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Ticket ticket = new Ticket();
        ticket.setTicketNumber(nextTicketNumber());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setCategory(category);
        ticket.setCreatedBy(creator);
        ticket.setLocation(request.location());
        ticket.setPreferredContact(request.preferredContact() == null ? "EMAIL" : request.preferredContact());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setSlaDeadline(slaService.computeDeadline(request.priority(), ticket.getCreatedAt()));
        ticketRepository.save(ticket);

        historyRepository.save(new TicketHistory(ticket, creator, "CREATED",
                "Ticket created with priority " + ticket.getPriority()));
        notificationService.notify(creator, ticket, NotificationType.TICKET_CREATED,
                "Your ticket " + ticket.getTicketNumber() + " was created");
        return toResponse(ticket, false, null, List.of());
    }

    private synchronized String nextTicketNumber() {
        String prefix = "INC-" + LocalDate.now().getYear() + "-";
        String max = ticketRepository.findMaxTicketNumberWithPrefix(prefix);
        long next = 1;
        if (max != null) {
            next = Long.parseLong(max.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%06d", next);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> list(String search, TicketStatus status, Priority priority,
                                             Long categoryId, String assignedTo, String createdBy,
                                             String sla, Instant createdFrom, Instant createdTo,
                                             int page, int size, String sortBy, String sortDir,
                                             User currentUser) {
        Specification<Ticket> spec = Specification
                .where(TicketSpecifications.search(search))
                .and(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.inCategory(categoryId))
                .and(TicketSpecifications.createdBetween(createdFrom, createdTo));

        if (currentUser.getRole() == Role.EMPLOYEE) {
            spec = spec.and(TicketSpecifications.createdBy(currentUser.getId()));
        } else if ("me".equals(createdBy)) {
            spec = spec.and(TicketSpecifications.createdBy(currentUser.getId()));
        } else if (createdBy != null && !createdBy.isBlank()) {
            spec = spec.and(TicketSpecifications.createdBy(Long.parseLong(createdBy)));
        }

        if ("unassigned".equals(assignedTo)) {
            spec = spec.and(TicketSpecifications.unassigned());
        } else if ("me".equals(assignedTo)) {
            spec = spec.and(TicketSpecifications.assignedTo(currentUser.getId()));
        } else if (assignedTo != null && !assignedTo.isBlank()) {
            spec = spec.and(TicketSpecifications.assignedTo(Long.parseLong(assignedTo)));
        }

        if ("breached".equalsIgnoreCase(sla)) {
            spec = spec.and(TicketSpecifications.slaBreached());
        } else if ("at_risk".equalsIgnoreCase(sla)) {
            spec = spec.and(TicketSpecifications.slaAtRisk());
        }

        String property = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(property).ascending() : Sort.by(property).descending();
        Page<Ticket> tickets = ticketRepository.findAll(spec, PageRequest.of(page, Math.min(size, 100), sort));

        Map<Long, Rating> ratings = ratingsFor(tickets.getContent());
        Page<TicketResponse> mapped = tickets.map(t -> toResponse(t,
                ratings.containsKey(t.getId()),
                ratings.containsKey(t.getId()) ? ratings.get(t.getId()).getStars() : null,
                List.of()));
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse detail(Long id, User currentUser) {
        Ticket ticket = findAccessible(id, currentUser);
        List<CommentResponse> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(id).stream()
                .map(CommentResponse::from).toList();
        List<HistoryResponse> history = historyRepository.findByTicketIdOrderByCreatedAtAsc(id).stream()
                .map(HistoryResponse::from).toList();
        Rating rating = ratingRepository.findByTicketId(id).orElse(null);
        List<AttachmentResponse> attachments = attachmentRepository.findByTicketIdOrderByCreatedAtAsc(id).stream()
                .map(AttachmentResponse::from).toList();
        TicketResponse ticketResponse = toResponse(ticket, rating != null,
                rating == null ? null : rating.getStars(), attachments);
        return new TicketDetailResponse(ticketResponse, comments, history);
    }

    @Transactional(readOnly = true)
    public Ticket findAccessible(Long id, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (currentUser.getRole() == Role.EMPLOYEE && !ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("You can only access your own tickets");
        }
        return ticket;
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request, User currentUser) {
        Ticket ticket = findAccessible(id, currentUser);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isAdmin && ticket.getStatus() != TicketStatus.OPEN) {
            throw new ForbiddenActionException("Only admins can edit a ticket after it leaves OPEN status");
        }
        List<String> changes = new ArrayList<>();
        if (request.subject() != null && !request.subject().equals(ticket.getSubject())) {
            changes.add("subject");
            ticket.setSubject(request.subject().trim());
        }
        if (request.description() != null && !request.description().equals(ticket.getDescription())) {
            changes.add("description");
            ticket.setDescription(request.description().trim());
        }
        if (request.categoryId() != null && !request.categoryId().equals(ticket.getCategory().getId())) {
            ticket.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
            changes.add("category");
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            applyPriority(ticket, request.priority(), currentUser);
            changes.add("priority");
        }
        if (request.location() != null) {
            ticket.setLocation(request.location());
        }
        if (request.preferredContact() != null) {
            ticket.setPreferredContact(request.preferredContact());
        }
        if (!changes.isEmpty()) {
            historyRepository.save(new TicketHistory(ticket, currentUser, "UPDATED",
                    "Updated " + String.join(", ", changes)));
        }
        return toResponse(ticket, ratingRepository.existsByTicketId(id), null, attachmentsOf(id));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, StatusUpdateRequest request, User currentUser) {
        Ticket ticket = findAccessible(id, currentUser);
        TicketWorkflow.validate(currentUser.getRole(), currentUser, ticket, request.status());

        if (request.status() == TicketStatus.RESOLVED
                && (request.note() == null || request.note().isBlank())) {
            throw new InvalidTicketStatusException("A resolution note is required when resolving a ticket");
        }

        TicketStatus from = ticket.getStatus();
        ticket.setStatus(request.status());

        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
            ticket.setResolutionNote(request.note().trim());
        }
        if (request.status() == TicketStatus.REOPENED) {
            ticket.setResolvedAt(null);
            ticket.setResolutionNote(null);
            ticket.setSlaBreachedNotified(false);
            ticket.setSlaAtRiskNotified(false);
        }
        if (request.status() == TicketStatus.IN_PROGRESS && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(Instant.now());
        }

        historyRepository.save(new TicketHistory(ticket, currentUser, "STATUS_CHANGED",
                from + " -> " + request.status() + (request.note() != null && !request.note().isBlank()
                        ? " (" + request.note().trim() + ")" : "")));

        notifyStatusChange(ticket, currentUser, from, request.status());
        return toResponse(ticket, ratingRepository.existsByTicketId(id), null, attachmentsOf(id));
    }

    private void notifyStatusChange(Ticket ticket, User actor, TicketStatus from, TicketStatus to) {
        NotificationType type = switch (to) {
            case RESOLVED -> NotificationType.TICKET_RESOLVED;
            case REOPENED -> NotificationType.TICKET_REOPENED;
            case ESCALATED -> NotificationType.TICKET_ESCALATED;
            default -> NotificationType.STATUS_CHANGED;
        };
        String message = "Ticket " + ticket.getTicketNumber() + " moved from " + from + " to " + to;

        User creator = ticket.getCreatedBy();
        User agent = ticket.getAssignedAgent();
        if (!creator.getId().equals(actor.getId())) {
            notificationService.notify(creator, ticket, type, message);
        }
        if (agent != null && !agent.getId().equals(actor.getId())) {
            notificationService.notify(agent, ticket, type, message);
        }
        if (to == TicketStatus.ESCALATED) {
            for (User admin : userRepository.findByRoleAndActiveTrue(Role.ADMIN)) {
                if (!admin.getId().equals(actor.getId())) {
                    notificationService.notify(admin, ticket, type,
                            "Ticket " + ticket.getTicketNumber() + " was escalated");
                }
            }
        }
    }

    @Transactional
    public TicketResponse assign(Long id, AssignTicketRequest request, User currentUser) {
        Ticket ticket = findAccessible(id, currentUser);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenActionException("Only admins can assign tickets");
        }
        User agent = userRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        if (agent.getRole() != Role.SUPPORT_AGENT) {
            throw new InvalidTicketStatusException("User " + agent.getEmail() + " is not a support agent");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new com.itserve.exception.TicketAlreadyClosedException(
                    "Ticket " + ticket.getTicketNumber() + " is closed");
        }

        User previous = ticket.getAssignedAgent();
        ticket.setAssignedAgent(agent);
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.ESCALATED
                || ticket.getStatus() == TicketStatus.REOPENED) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        historyRepository.save(new TicketHistory(ticket, currentUser, "ASSIGNED",
                (previous == null ? "Assigned to " : "Reassigned from " + previous.getFullName() + " to ")
                        + agent.getFullName()));
        notificationService.notify(agent, ticket, NotificationType.TICKET_ASSIGNED,
                "Ticket " + ticket.getTicketNumber() + " assigned to you");
        notificationService.notify(ticket.getCreatedBy(), ticket, NotificationType.TICKET_ASSIGNED,
                "Your ticket " + ticket.getTicketNumber() + " was assigned to " + agent.getFullName());
        return toResponse(ticket, ratingRepository.existsByTicketId(id), null, attachmentsOf(id));
    }

    @Transactional
    public TicketResponse updatePriority(Long id, PriorityUpdateRequest request, User currentUser) {
        Ticket ticket = findAccessible(id, currentUser);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenActionException("Only admins can change priority");
        }
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new InvalidTicketStatusException("Cannot change priority of a " + ticket.getStatus() + " ticket");
        }
        applyPriority(ticket, request.priority(), currentUser);
        return toResponse(ticket, ratingRepository.existsByTicketId(id), null, attachmentsOf(id));
    }

    private void applyPriority(Ticket ticket, Priority priority, User actor) {
        Priority old = ticket.getPriority();
        ticket.setPriority(priority);
        ticket.setSlaDeadline(slaService.computeDeadline(priority, ticket.getCreatedAt()));
        ticket.setSlaAtRiskNotified(false);
        ticket.setSlaBreachedNotified(false);
        historyRepository.save(new TicketHistory(ticket, actor, "PRIORITY_CHANGED",
                old + " -> " + priority + ", SLA deadline recalculated"));
    }

    @Transactional
    public TicketResponse escalate(Long id, StatusUpdateRequest request, User currentUser) {
        Ticket ticket = findAccessible(id, currentUser);
        TicketWorkflow.validate(currentUser.getRole(), currentUser, ticket, TicketStatus.ESCALATED);
        TicketStatus from = ticket.getStatus();
        ticket.setStatus(TicketStatus.ESCALATED);
        historyRepository.save(new TicketHistory(ticket, currentUser, "ESCALATED",
                request.note() == null || request.note().isBlank() ? "Ticket escalated" : request.note().trim()));
        notifyStatusChange(ticket, currentUser, from, TicketStatus.ESCALATED);
        return toResponse(ticket, ratingRepository.existsByTicketId(id), null, attachmentsOf(id));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenActionException("Only admins can delete tickets");
        }
        ratingRepository.findByTicketId(id).ifPresent(ratingRepository::delete);
        notificationRepository.findAll().stream()
                .filter(n -> n.getTicket() != null && n.getTicket().getId().equals(id))
                .forEach(notificationRepository::delete);
        commentRepository.findByTicketIdOrderByCreatedAtAsc(id).forEach(commentRepository::delete);
        historyRepository.findByTicketIdOrderByCreatedAtAsc(id).forEach(historyRepository::delete);
        attachmentRepository.findByTicketIdOrderByCreatedAtAsc(id).forEach(attachmentRepository::delete);
        ticketRepository.delete(ticket);
    }

    @Transactional(readOnly = true)
    public List<SuggestedAgentResponse> suggestedAgents(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        String categoryName = ticket.getCategory().getName();

        List<User> agents = userRepository.findByRoleAndActiveTrue(Role.SUPPORT_AGENT);
        Map<Long, Long> activeCounts = ticketRepository
                .countActiveGroupByAgent(TicketSpecifications.ACTIVE_STATUSES).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Instant now = Instant.now();

        List<SuggestedAgentResponse> suggestions = new ArrayList<>();
        for (User agent : agents) {
            List<String> skills = parseSkills(agent.getSkills());
            boolean skillMatch = skills.stream().anyMatch(s -> s.equalsIgnoreCase(categoryName));
            long active = activeCounts.getOrDefault(agent.getId(), 0L);
            long breached = ticketRepository.countByAssignedAgentIdAndStatusInAndSlaDeadlineBefore(
                    agent.getId(), TicketSpecifications.ACTIVE_STATUSES, now);
            suggestions.add(new SuggestedAgentResponse(agent.getId(), agent.getFullName(), skills,
                    skillMatch, active, breached));
        }
        // Deterministic ranking: skill match first, then lowest active workload, then name.
        suggestions.sort(Comparator
                .comparing(SuggestedAgentResponse::skillMatch).reversed()
                .thenComparingLong(SuggestedAgentResponse::activeTickets)
                .thenComparing(SuggestedAgentResponse::fullName));
        return suggestions;
    }

    @Transactional(readOnly = true)
    public List<AgentWorkloadResponse> agentWorkloads() {
        Instant now = Instant.now();
        List<User> agents = userRepository.findByRole(Role.SUPPORT_AGENT);
        List<AgentWorkloadResponse> result = new ArrayList<>();
        for (User agent : agents) {
            long assigned = ticketRepository.countByAssignedAgentIdAndStatusIn(
                    agent.getId(), TicketSpecifications.ACTIVE_STATUSES);
            long open = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(),
                    List.of(TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.ESCALATED, TicketStatus.REOPENED));
            long inProgress = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(),
                    List.of(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_USER));
            long resolved = ticketRepository.countByAssignedAgentIdAndStatusIn(agent.getId(),
                    List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
            long breached = ticketRepository.countByAssignedAgentIdAndStatusInAndSlaDeadlineBefore(
                    agent.getId(), TicketSpecifications.ACTIVE_STATUSES, now);
            result.add(new AgentWorkloadResponse(agent.getId(), agent.getFullName(), agent.getEmail(),
                    parseSkills(agent.getSkills()), assigned, open, inProgress, resolved, breached));
        }
        return result;
    }

    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, User currentUser) {
        Ticket ticket = findAccessible(ticketId, currentUser);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new com.itserve.exception.TicketAlreadyClosedException("Cannot attach files to a closed ticket");
        }
        if (file.isEmpty()) {
            throw new InvalidTicketStatusException("Attachment file is empty");
        }
        Attachment attachment = new Attachment();
        attachment.setTicket(ticket);
        attachment.setUploadedBy(currentUser);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        try {
            attachment.setData(file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
        attachmentRepository.save(attachment);
        historyRepository.save(new TicketHistory(ticket, currentUser, "ATTACHMENT_ADDED",
                "Attached " + attachment.getFileName()));
        return AttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachmentId, User currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        findAccessible(attachment.getTicket().getId(), currentUser);
        return attachment;
    }

    private Map<Long, Rating> ratingsFor(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = tickets.stream().map(Ticket::getId).toList();
        return ratingRepository.findByTicketIdIn(ids).stream()
                .collect(Collectors.toMap(r -> r.getTicket().getId(), Function.identity()));
    }

    private List<AttachmentResponse> attachmentsOf(Long ticketId) {
        return attachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(AttachmentResponse::from).toList();
    }

    static List<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(skills.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private TicketResponse toResponse(Ticket ticket, boolean rated, Integer stars, List<AttachmentResponse> attachments) {
        if (rated && stars == null) {
            Rating rating = ratingRepository.findByTicketId(ticket.getId()).orElse(null);
            stars = rating == null ? null : rating.getStars();
        }
        return TicketResponse.from(ticket, rated, stars, attachments);
    }

    @Transactional(readOnly = true)
    public List<Ticket> recentTicketsFor(User user) {
        return user.getRole() == Role.EMPLOYEE
                ? ticketRepository.findTop10ByCreatedByIdOrderByCreatedAtDesc(user.getId())
                : ticketRepository.findTop10ByOrderByCreatedAtDesc();
    }

    static Instant startOfDay(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
