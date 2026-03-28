CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Root categories
INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Electronics', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Electronics' AND parent_id IS NULL
);

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Vehicles', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Vehicles' AND parent_id IS NULL
);

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Property', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Property' AND parent_id IS NULL
);

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Home and Garden', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Home and Garden' AND parent_id IS NULL
);

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Fashion', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Fashion' AND parent_id IS NULL
);

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Sports and Outdoors', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Sports and Outdoors' AND parent_id IS NULL
);

-- Electronics sub-categories
INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Phones', p.id
FROM category p
WHERE p.name = 'Electronics' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'Phones' AND c.parent_id = p.id
  );

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Computers', p.id
FROM category p
WHERE p.name = 'Electronics' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'Computers' AND c.parent_id = p.id
  );

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'TV and Audio', p.id
FROM category p
WHERE p.name = 'Electronics' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'TV and Audio' AND c.parent_id = p.id
  );

-- Vehicles sub-categories
INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Cars', p.id
FROM category p
WHERE p.name = 'Vehicles' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'Cars' AND c.parent_id = p.id
  );

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Motorcycles', p.id
FROM category p
WHERE p.name = 'Vehicles' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'Motorcycles' AND c.parent_id = p.id
  );

-- Property sub-categories
INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Apartments', p.id
FROM category p
WHERE p.name = 'Property' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'Apartments' AND c.parent_id = p.id
  );

INSERT INTO category (id, name, parent_id)
SELECT gen_random_uuid(), 'Houses', p.id
FROM category p
WHERE p.name = 'Property' AND p.parent_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.name = 'Houses' AND c.parent_id = p.id
  );


