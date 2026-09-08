-- Core accounts ledger: schema + seed data.
--
-- Shared by BOTH implementations (java/ and python/). Loaded into an in-memory
-- database on every start, so the data is identical and fresh on every run.
--
-- Conventions (see docs/data-dictionary.md):
--   * Money is stored as integer cents. Never a float, never a decimal string.
--   * amount_cents is ALWAYS positive; the sign of a transaction comes from `type`.
--   * posted_at is an ISO-8601 UTC timestamp string, e.g. 2025-01-31T14:30:00Z.
--
-- All data below is synthetic. It is not, and must never be, real member data.

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS members;

CREATE TABLE members (
    id        VARCHAR(16)  NOT NULL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email     VARCHAR(160) NOT NULL
);

CREATE TABLE accounts (
    id                    VARCHAR(16) NOT NULL PRIMARY KEY,
    member_id             VARCHAR(16) NOT NULL,
    account_number        VARCHAR(12) NOT NULL,
    type                  VARCHAR(16) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS')),
    currency              VARCHAR(3)  NOT NULL,
    status                VARCHAR(16) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    -- Balance as of 2025-01-01T00:00:00Z, before any transaction in this table.
    opening_balance_cents BIGINT      NOT NULL,
    FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE transactions (
    id           INTEGER      NOT NULL PRIMARY KEY,
    account_id   VARCHAR(16)  NOT NULL,
    posted_at    VARCHAR(20)  NOT NULL,
    type         VARCHAR(16)  NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    amount_cents BIGINT       NOT NULL CHECK (amount_cents > 0),
    description  VARCHAR(120) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts (id)
);

INSERT INTO members (id, full_name, email) VALUES ('MEM-1', 'Ana Ramirez', 'ana.ramirez@example.org');
INSERT INTO members (id, full_name, email) VALUES ('MEM-2', 'Luis Ortega', 'luis.ortega@example.org');
INSERT INTO members (id, full_name, email) VALUES ('MEM-3', 'Marisol Vega', 'marisol.vega@example.org');

INSERT INTO accounts (id, member_id, account_number, type, currency, status, opening_balance_cents) VALUES ('ACC-1001', 'MEM-1', '483920174821', 'CHECKING', 'USD', 'OPEN',    152000);
INSERT INTO accounts (id, member_id, account_number, type, currency, status, opening_balance_cents) VALUES ('ACC-1002', 'MEM-1', '483920175530', 'SAVINGS',  'USD', 'OPEN',   2500000);
INSERT INTO accounts (id, member_id, account_number, type, currency, status, opening_balance_cents) VALUES ('ACC-1003', 'MEM-2', '672104558817', 'CHECKING', 'USD', 'OPEN',     87500);
INSERT INTO accounts (id, member_id, account_number, type, currency, status, opening_balance_cents) VALUES ('ACC-1004', 'MEM-2', '672104559243', 'SAVINGS',  'USD', 'CLOSED',   15000);
INSERT INTO accounts (id, member_id, account_number, type, currency, status, opening_balance_cents) VALUES ('ACC-1005', 'MEM-3', '915630042276', 'CHECKING', 'EUR', 'OPEN',    340075);

-- Transactions are stored in id order, which is NOT time order.
-- Anything that needs chronological order has to ask for it.
INSERT INTO transactions (id, account_id, posted_at, type, amount_cents, description) VALUES
    ( 1, 'ACC-1002', '2025-01-10T12:00:00Z', 'DEBIT',  1000000, 'WIRE OUT TITLE COMPANY 8830'),
    ( 2, 'ACC-1003', '2025-01-02T08:15:00Z', 'CREDIT',  210000, 'ACH CREDIT PAYROLL ACME CORP'),
    ( 3, 'ACC-1001', '2025-01-31T14:30:00Z', 'CREDIT', 1250000, 'ACH CREDIT PAYROLL ACME CORP'),
    ( 4, 'ACC-1005', '2025-01-07T10:00:00Z', 'DEBIT',     2500, 'POS DEBIT COFFEE CO'),
    ( 5, 'ACC-1003', '2025-01-14T09:00:00Z', 'DEBIT',     7800, 'POS DEBIT HARDWARE STORE'),
    ( 6, 'ACC-1002', '2025-01-24T15:20:00Z', 'CREDIT', 1500000, 'WIRE IN ESCROW RELEASE'),
    ( 7, 'ACC-1004', '2024-12-05T09:00:00Z', 'CREDIT',    50000, 'MOBILE DEPOSIT'),
    ( 8, 'ACC-1001', '2025-01-28T10:30:00Z', 'CREDIT',     1250, 'DIVIDEND CREDIT'),
    ( 9, 'ACC-1003', '2025-01-16T18:40:00Z', 'DEBIT',      5400, 'POS DEBIT COFFEE CO'),
    (10, 'ACC-1005', '2025-01-13T09:30:00Z', 'CREDIT',   180000, 'ACH CREDIT PAYROLL ACME CORP'),
    (11, 'ACC-1002', '2025-01-05T07:30:00Z', 'CREDIT',    45000, 'ACH CREDIT TRANSFER FROM CHECKING'),
    (12, 'ACC-1001', '2024-12-28T13:05:00Z', 'DEBIT',      8900, 'POS DEBIT GROCERY MART'),
    (13, 'ACC-1005', '2025-01-20T15:00:00Z', 'DEBIT',      1200, 'MONTHLY MAINTENANCE FEE'),
    (14, 'ACC-1003', '2024-12-30T16:00:00Z', 'DEBIT',      3300, 'POS DEBIT GROCERY MART'),
    (15, 'ACC-1002', '2024-12-20T10:00:00Z', 'CREDIT',    60000, 'MOBILE DEPOSIT'),
    (16, 'ACC-1004', '2024-12-15T14:30:00Z', 'DEBIT',      1200, 'MONTHLY MAINTENANCE FEE'),
    (17, 'ACC-1001', '2025-01-03T09:12:00Z', 'DEBIT',      4500, 'POS DEBIT COFFEE CO'),
    (18, 'ACC-1005', '2024-12-29T11:20:00Z', 'DEBIT',      4400, 'POS DEBIT GROCERY MART'),
    (19, 'ACC-1002', '2025-01-31T23:59:59Z', 'CREDIT',     3125, 'DIVIDEND CREDIT'),
    (20, 'ACC-1003', '2025-01-09T13:25:00Z', 'DEBIT',      1200, 'MONTHLY MAINTENANCE FEE'),
    (21, 'ACC-1001', '2025-02-01T02:00:00Z', 'DEBIT',      6200, 'POS DEBIT GAS STATION 12'),
    (22, 'ACC-1005', '2025-01-26T17:35:00Z', 'DEBIT',      8850, 'POS DEBIT HARDWARE STORE'),
    (23, 'ACC-1003', '2025-01-23T11:10:00Z', 'DEBIT',     45000, 'LOAN PAYMENT AUTO 4471'),
    (24, 'ACC-1002', '2025-01-18T09:45:00Z', 'DEBIT',      2000, 'ATM WITHDRAWAL FEE'),
    (25, 'ACC-1005', '2025-02-02T08:45:00Z', 'CREDIT',     1500, 'DIVIDEND CREDIT'),
    (26, 'ACC-1001', '2025-01-21T16:45:00Z', 'DEBIT',      1750, 'WIRE TRANSFER FEE'),
    (27, 'ACC-1004', '2024-12-26T09:10:00Z', 'CREDIT',      425, 'DIVIDEND CREDIT'),
    (28, 'ACC-1003', '2025-01-30T07:55:00Z', 'CREDIT',   210000, 'ACH CREDIT PAYROLL ACME CORP'),
    (29, 'ACC-1001', '2025-01-15T08:00:00Z', 'DEBIT',     15000, 'LOAN PAYMENT AUTO 4471'),
    (30, 'ACC-1002', '2025-02-05T11:15:00Z', 'DEBIT',     35000, 'LOAN PAYMENT AUTO 4471'),
    (31, 'ACC-1003', '2025-01-14T09:00:00Z', 'DEBIT',      2500, 'POS DEBIT PHARMACY 221'),
    (32, 'ACC-1005', '2025-01-11T13:15:00Z', 'DEBIT',      3200, 'WIRE TRANSFER FEE'),
    (33, 'ACC-1001', '2024-12-31T23:45:00Z', 'CREDIT',    25000, 'MOBILE DEPOSIT'),
    (34, 'ACC-1002', '2025-01-12T14:00:00Z', 'DEBIT',     12500, 'POS DEBIT HOME SUPPLY'),
    (35, 'ACC-1003', '2025-02-04T10:05:00Z', 'DEBIT',      6600, 'POS DEBIT GAS STATION 12'),
    (36, 'ACC-1004', '2024-12-27T10:45:00Z', 'DEBIT',     64225, 'WIRE OUT ACCOUNT CLOSURE'),
    (37, 'ACC-1001', '2025-01-08T11:05:00Z', 'DEBIT',      1200, 'MONTHLY MAINTENANCE FEE'),
    (38, 'ACC-1002', '2025-01-27T08:20:00Z', 'DEBIT',      7500, 'POS DEBIT PHARMACY 221'),
    (39, 'ACC-1003', '2025-01-06T12:30:00Z', 'DEBIT',      9900, 'WIRE OUT FAMILY SUPPORT'),
    (40, 'ACC-1001', '2025-02-03T09:00:00Z', 'CREDIT',     5000, 'ATM DEPOSIT');
