-- name: find-property
-- Find the property for the given SOURCE and CODE.
SELECT pk_property_id
FROM property
WHERE source = :source AND code = :code
