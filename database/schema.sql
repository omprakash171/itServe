-- ITServe — Enterprise IT Service Desk
-- MySQL 8 schema (matches the JPA entities; Hibernate uses Spring's
-- default naming strategy: camelCase -> snake_case).
--
-- For local demos the app runs on an in-memory H2 database and creates
-- this schema automatically (ddl-auto: create-drop). Use this script
-- when running with the `mysql` profile (ddl-auto: validate/update).

CREATE DATABASE IF NOT EXISTS itserve CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE itserve;

CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,              -- ADMIN | SUPPORT_AGENT | EMPLOYEE
    department  VARCHAR(255),
    skills      VARCHAR(255),                       -- comma-separated category names (agents)
    active      BIT(1)       NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT idx_users_email UNIQUE (email)
) ENGINE = InnoDB;
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE = InnoDB;

CREATE TABLE sla_rules (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    priority         VARCHAR(10) NOT NULL UNIQUE,   -- LOW | MEDIUM | HIGH | CRITICAL
    resolution_hours INT         NOT NULL
) ENGINE = InnoDB;

CREATE TABLE tickets (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number         VARCHAR(20)   NOT NULL UNIQUE,   -- INC-YYYY-NNNNNN
    subject               VARCHAR(200)  NOT NULL,
    description           VARCHAR(4000) NOT NULL,
    status                VARCHAR(20)   NOT NULL,          -- OPEN/ASSIGNED/IN_PROGRESS/...
    priority              VARCHAR(10)   NOT NULL,
    category_id           BIGINT        NOT NULL,
    created_by_id         BIGINT        NOT NULL,
    assigned_agent_id     BIGINT,
    location              VARCHAR(255),
    preferred_contact     VARCHAR(20),                     -- EMAIL | PHONE | CHAT
    sla_deadline          DATETIME(6),
    sla_breached_notified BIT(1) NOT NULL DEFAULT 0,
    sla_at_risk_notified  BIT(1) NOT NULL DEFAULT 0,
    first_response_at     DATETIME(6),
    resolved_at           DATETIME(6),
    resolution_note       VARCHAR(2000),
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    CONSTRAINT fk_tickets_category       FOREIGN KEY (category_id)       REFERENCES categories (id),
    CONSTRAINT fk_tickets_created_by     FOREIGN KEY (created_by_id)     REFERENCES users (id),
    CONSTRAINT fk_tickets_assigned_agent FOREIGN KEY (assigned_agent_id) REFERENCES users (id)
) ENGINE = InnoDB;
CREATE INDEX idx_tickets_number          ON tickets (ticket_number);
CREATE INDEX idx_tickets_status          ON tickets (status);
CREATE INDEX idx_tickets_priority        ON tickets (priority);
CREATE INDEX idx_tickets_created_at      ON tickets (created_at);
CREATE INDEX idx_tickets_assigned_agent  ON tickets (assigned_agent_id);
CREATE INDEX idx_tickets_category        ON tickets (category_id);
CREATE INDEX idx_tickets_created_by      ON tickets (created_by_id);

CREATE TABLE comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB;
CREATE INDEX idx_comments_ticket ON comments (ticket_id);

CREATE TABLE attachments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id      BIGINT       NOT NULL,
    uploaded_by_id BIGINT       NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(255),
    size_bytes     BIGINT       NOT NULL,
    file_data      LONGBLOB,
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_attachments_ticket      FOREIGN KEY (ticket_id)      REFERENCES tickets (id),
    CONSTRAINT fk_attachments_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users (id)
) ENGINE = InnoDB;
CREATE INDEX idx_attachments_ticket ON attachments (ticket_id);

CREATE TABLE notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT       NOT NULL,
    ticket_id    BIGINT,
    type         VARCHAR(30)  NOT NULL,
    message      VARCHAR(300) NOT NULL,
    is_read      BIT(1)       NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_ticket    FOREIGN KEY (ticket_id)    REFERENCES tickets (id)
) ENGINE = InnoDB;
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id);
CREATE INDEX idx_notifications_read      ON notifications (is_read);

CREATE TABLE knowledge_articles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    category    VARCHAR(50)   NOT NULL,
    content     VARCHAR(8000) NOT NULL,
    author_id   BIGINT        NOT NULL,
    helpful_yes INT DEFAULT 0,
    helpful_no  INT DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB;
CREATE INDEX idx_articles_category ON knowledge_articles (category);

CREATE TABLE ratings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT UNIQUE NOT NULL,
    created_by_id BIGINT        NOT NULL,
    stars         INT           NOT NULL,           -- 1..5
    feedback      VARCHAR(1000),
    created_at    DATETIME(6)   NOT NULL,
    CONSTRAINT fk_ratings_ticket     FOREIGN KEY (ticket_id)     REFERENCES tickets (id),
    CONSTRAINT fk_ratings_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
) ENGINE = InnoDB;
CREATE INDEX idx_ratings_ticket ON ratings (ticket_id);

CREATE TABLE ticket_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT       NOT NULL,
    performed_by_id BIGINT       NOT NULL,
    action          VARCHAR(30)  NOT NULL,           -- CREATED/ASSIGNED/STATUS_CHANGED/...
    detail          VARCHAR(500),
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_history_ticket       FOREIGN KEY (ticket_id)       REFERENCES tickets (id),
    CONSTRAINT fk_history_performed_by FOREIGN KEY (performed_by_id) REFERENCES users (id)
) ENGINE = InnoDB;
CREATE INDEX idx_history_ticket ON ticket_history (ticket_id);
