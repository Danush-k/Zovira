-- Reference data required in every environment: roles, permissions and platform defaults.

INSERT INTO roles (name, description)
VALUES ('CUSTOMER', 'Browses and purchases products'),
       ('SELLER', 'Lists products and fulfils orders'),
       ('ADMIN', 'Operates and moderates the platform');

INSERT INTO permissions (name, description)
VALUES ('catalog:manage-own', 'Create and edit own product listings'),
       ('inventory:manage-own', 'Adjust stock for own listings'),
       ('orders:fulfil-own', 'Process and ship own order items'),
       ('returns:resolve-own', 'Approve or reject returns for own items'),
       ('analytics:view-own', 'View own sales analytics'),
       ('catalog:manage-all', 'Manage any product, category or brand'),
       ('orders:manage-all', 'View and update any order'),
       ('users:manage', 'Manage user accounts and roles'),
       ('sellers:manage', 'Approve, reject and suspend sellers'),
       ('reviews:moderate', 'Moderate product reviews'),
       ('coupons:manage', 'Create and edit coupons'),
       ('payments:view', 'View payments, refunds and returns'),
       ('settings:manage', 'Edit platform settings'),
       ('analytics:view-platform', 'View platform analytics'),
       ('audit:view', 'View the audit log');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN ('catalog:manage-own', 'inventory:manage-own', 'orders:fulfil-own',
                                          'returns:resolve-own', 'analytics:view-own')
WHERE r.name = 'SELLER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN ('catalog:manage-all', 'orders:manage-all', 'users:manage',
                                          'sellers:manage', 'reviews:moderate', 'coupons:manage', 'payments:view',
                                          'settings:manage', 'analytics:view-platform', 'audit:view')
WHERE r.name = 'ADMIN';

INSERT INTO platform_settings (key, value, description)
VALUES ('shipping.free-threshold', '499', 'Cart value (INR) at or above which standard delivery is free'),
       ('shipping.standard-fee', '40', 'Standard delivery fee (INR) below the free threshold'),
       ('shipping.express-fee', '99', 'Express delivery fee (INR)'),
       ('shipping.standard-days', '4', 'Standard delivery estimate in days'),
       ('shipping.express-days', '1', 'Express delivery estimate in days'),
       ('payments.cod-enabled', 'true', 'Whether cash on delivery is offered'),
       ('payments.cod-fee', '0', 'Cash on delivery handling fee (INR)'),
       ('payments.cod-max-order', '50000', 'Maximum order value (INR) eligible for cash on delivery'),
       ('orders.max-quantity-per-item', '10', 'Maximum quantity of a single item per order'),
       ('reviews.auto-publish', 'true', 'Publish reviews immediately instead of holding for moderation');
