INSERT INTO stream_categories (name, display_order)
VALUES
    ('Music', 10),
    ('Gaming', 20),
    ('Talk Show', 30),
    ('Education', 40),
    ('Lifestyle', 50),
    ('Dance', 60),
    ('Comedy', 70)
ON CONFLICT (name) DO NOTHING;
