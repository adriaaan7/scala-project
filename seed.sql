-- =============================================================
-- Seed data — 4 trips, 2 per user, mixed participant combos
-- User A: 6765015f-a5bd-4088-afcc-0e7eeed43dd8
-- User B: aaff1398-45d5-4220-a531-65df507dca6d
-- =============================================================

-- -------------------------------------------------------------
-- TRIPS
-- Trip 1: Paris      — User A, no participants
-- Trip 2: Italy      — User A, User B is participant
-- Trip 3: Tokyo      — User B, no participants
-- Trip 4: New York   — User B, User A is participant
-- -------------------------------------------------------------

INSERT INTO trips (id, title, start_date, end_date, owner_id) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Paris Getaway',       '2025-03-10', '2025-03-17', '6765015f-a5bd-4088-afcc-0e7eeed43dd8'),
  ('22222222-2222-2222-2222-222222222222', 'Italy with Friends',  '2025-06-01', '2025-06-10', '6765015f-a5bd-4088-afcc-0e7eeed43dd8'),
  ('33333333-3333-3333-3333-333333333333', 'Tokyo Adventure',     '2025-09-05', '2025-09-15', 'aaff1398-45d5-4220-a531-65df507dca6d'),
  ('44444444-4444-4444-4444-444444444444', 'New York City',       '2025-11-20', '2025-11-27', 'aaff1398-45d5-4220-a531-65df507dca6d')
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------
-- PARTICIPANTS
-- Trip 2: User B joins User A's Italy trip
-- Trip 4: User A joins User B's NYC trip
-- -------------------------------------------------------------

INSERT INTO trip_participants (id, trip_id, user_id) VALUES
  ('aa000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'aaff1398-45d5-4220-a531-65df507dca6d'),
  ('aa000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', '6765015f-a5bd-4088-afcc-0e7eeed43dd8')
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------
-- PLACES — Trip 1: Paris Getaway  (Mar 10–17, User A solo)
-- 8 places across 7 days; two stops on Mar 13
-- -------------------------------------------------------------

INSERT INTO places (id, trip_id, name, description, lat, lng, start_date, end_date) VALUES

  ('a1000000-0000-0000-0000-000000000001',
   '11111111-1111-1111-1111-111111111111',
   'Eiffel Tower',
   'Iconic iron lattice tower on the Champ de Mars',
   48.8584, 2.2945,
   '2025-03-11 10:00:00+00', '2025-03-11 14:00:00+00'),

  ('a1000000-0000-0000-0000-000000000002',
   '11111111-1111-1111-1111-111111111111',
   'Louvre Museum',
   'World''s largest art museum and historic monument',
   48.8606, 2.3376,
   '2025-03-12 09:00:00+00', '2025-03-12 17:00:00+00'),

  ('a1000000-0000-0000-0000-000000000003',
   '11111111-1111-1111-1111-111111111111',
   'Notre-Dame Cathedral',
   'Medieval Gothic cathedral on the Île de la Cité',
   48.8530, 2.3499,
   '2025-03-13 10:00:00+00', '2025-03-13 12:00:00+00'),

  ('a1000000-0000-0000-0000-000000000004',
   '11111111-1111-1111-1111-111111111111',
   'Arc de Triomphe',
   'Triumphal arch at the western end of the Champs-Élysées',
   48.8738, 2.2950,
   '2025-03-13 14:00:00+00', '2025-03-13 16:00:00+00'),

  ('a1000000-0000-0000-0000-000000000005',
   '11111111-1111-1111-1111-111111111111',
   'Musée d''Orsay',
   'Impressionist and Post-Impressionist art in a former railway station',
   48.8600, 2.3266,
   '2025-03-14 10:00:00+00', '2025-03-14 15:00:00+00'),

  ('a1000000-0000-0000-0000-000000000006',
   '11111111-1111-1111-1111-111111111111',
   'Sacré-Cœur Basilica',
   'Romano-Byzantine basilica atop Montmartre hill',
   48.8867, 2.3431,
   '2025-03-15 10:00:00+00', '2025-03-15 13:00:00+00'),

  ('a1000000-0000-0000-0000-000000000007',
   '11111111-1111-1111-1111-111111111111',
   'Palace of Versailles',
   'Royal château and gardens, UNESCO World Heritage Site',
   48.8049, 2.1204,
   '2025-03-16 09:00:00+00', '2025-03-16 17:00:00+00'),

  ('a1000000-0000-0000-0000-000000000008',
   '11111111-1111-1111-1111-111111111111',
   'Luxembourg Gardens',
   'Formal gardens surrounding the French Senate',
   48.8462, 2.3372,
   '2025-03-17 09:00:00+00', '2025-03-17 12:00:00+00')

ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------
-- PLACES — Trip 2: Italy with Friends  (Jun 1–10, User A + User B)
-- 10 places — Rome (Jun 2–5) then Florence (Jun 7–9)
-- -------------------------------------------------------------

INSERT INTO places (id, trip_id, name, description, lat, lng, start_date, end_date) VALUES

  ('b2000000-0000-0000-0000-000000000001',
   '22222222-2222-2222-2222-222222222222',
   'Colosseum',
   'Ancient Roman amphitheatre, the largest ever built',
   41.8902, 12.4922,
   '2025-06-02 09:00:00+00', '2025-06-02 13:00:00+00'),

  ('b2000000-0000-0000-0000-000000000002',
   '22222222-2222-2222-2222-222222222222',
   'Roman Forum',
   'Rectangular forum surrounded by ruins of ancient government buildings',
   41.8925, 12.4853,
   '2025-06-02 14:00:00+00', '2025-06-02 17:00:00+00'),

  ('b2000000-0000-0000-0000-000000000003',
   '22222222-2222-2222-2222-222222222222',
   'Vatican Museums',
   'Extensive collection of art and history amassed by the Catholic Church',
   41.9065, 12.4536,
   '2025-06-03 10:00:00+00', '2025-06-03 16:00:00+00'),

  ('b2000000-0000-0000-0000-000000000004',
   '22222222-2222-2222-2222-222222222222',
   'Trevi Fountain',
   'Baroque masterpiece and the largest fountain in Rome',
   41.9009, 12.4833,
   '2025-06-04 09:00:00+00', '2025-06-04 10:00:00+00'),

  ('b2000000-0000-0000-0000-000000000005',
   '22222222-2222-2222-2222-222222222222',
   'Pantheon',
   'Best-preserved ancient Roman building, now a church',
   41.8986, 12.4769,
   '2025-06-04 11:00:00+00', '2025-06-04 12:30:00+00'),

  ('b2000000-0000-0000-0000-000000000006',
   '22222222-2222-2222-2222-222222222222',
   'Borghese Gallery',
   'Art museum inside the Villa Borghese gardens',
   41.9143, 12.4921,
   '2025-06-05 10:00:00+00', '2025-06-05 13:00:00+00'),

  ('b2000000-0000-0000-0000-000000000007',
   '22222222-2222-2222-2222-222222222222',
   'Uffizi Gallery',
   'Renaissance art museum housing Botticelli and Leonardo works',
   43.7677, 11.2553,
   '2025-06-07 09:00:00+00', '2025-06-07 17:00:00+00'),

  ('b2000000-0000-0000-0000-000000000008',
   '22222222-2222-2222-2222-222222222222',
   'Ponte Vecchio',
   'Medieval stone arch bridge lined with jewellery shops',
   43.7680, 11.2530,
   '2025-06-08 10:00:00+00', '2025-06-08 11:30:00+00'),

  ('b2000000-0000-0000-0000-000000000009',
   '22222222-2222-2222-2222-222222222222',
   'Piazzale Michelangelo',
   'Panoramic terrace with sweeping views of Florence',
   43.7629, 11.2652,
   '2025-06-08 15:00:00+00', '2025-06-08 17:00:00+00'),

  ('b2000000-0000-0000-0000-000000000010',
   '22222222-2222-2222-2222-222222222222',
   'Florence Cathedral',
   'Gothic cathedral crowned by Brunelleschi''s iconic dome',
   43.7731, 11.2560,
   '2025-06-09 10:00:00+00', '2025-06-09 13:00:00+00')

ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------
-- PLACES — Trip 3: Tokyo Adventure  (Sep 5–15, User B solo)
-- 9 places spread across 8 days; two stops on Sep 7 and Sep 8
-- -------------------------------------------------------------

INSERT INTO places (id, trip_id, name, description, lat, lng, start_date, end_date) VALUES

  ('c3000000-0000-0000-0000-000000000001',
   '33333333-3333-3333-3333-333333333333',
   'Shinjuku Gyoen',
   'Large national garden with French, English and Japanese sections',
   35.6852, 139.7100,
   '2025-09-06 09:00:00+00', '2025-09-06 15:00:00+00'),

  ('c3000000-0000-0000-0000-000000000002',
   '33333333-3333-3333-3333-333333333333',
   'Meiji Shrine',
   'Shinto shrine dedicated to Emperor Meiji, set in forested grounds',
   35.6763, 139.6993,
   '2025-09-07 09:00:00+00', '2025-09-07 11:00:00+00'),

  ('c3000000-0000-0000-0000-000000000003',
   '33333333-3333-3333-3333-333333333333',
   'Harajuku Takeshita Street',
   'Pedestrian street famous for youth fashion and crepes',
   35.6703, 139.7027,
   '2025-09-07 12:00:00+00', '2025-09-07 14:00:00+00'),

  ('c3000000-0000-0000-0000-000000000004',
   '33333333-3333-3333-3333-333333333333',
   'Senso-ji Temple',
   'Tokyo''s oldest Buddhist temple in Asakusa',
   35.7147, 139.7966,
   '2025-09-08 08:00:00+00', '2025-09-08 11:00:00+00'),

  ('c3000000-0000-0000-0000-000000000005',
   '33333333-3333-3333-3333-333333333333',
   'Akihabara Electric Town',
   'District famous for electronics, anime and manga culture',
   35.7023, 139.7745,
   '2025-09-08 14:00:00+00', '2025-09-08 18:00:00+00'),

  ('c3000000-0000-0000-0000-000000000006',
   '33333333-3333-3333-3333-333333333333',
   'teamLab Borderless',
   'Immersive borderless digital art museum in Odaiba',
   35.6259, 139.7837,
   '2025-09-10 14:00:00+00', '2025-09-10 18:00:00+00'),

  ('c3000000-0000-0000-0000-000000000007',
   '33333333-3333-3333-3333-333333333333',
   'Tokyo Skytree',
   'World''s tallest tower with observation decks at 350 m and 450 m',
   35.7101, 139.8107,
   '2025-09-11 10:00:00+00', '2025-09-11 13:00:00+00'),

  ('c3000000-0000-0000-0000-000000000008',
   '33333333-3333-3333-3333-333333333333',
   'Tsukiji Outer Market',
   'Bustling market famous for fresh seafood and street food',
   35.6655, 139.7707,
   '2025-09-12 08:00:00+00', '2025-09-12 10:00:00+00'),

  ('c3000000-0000-0000-0000-000000000009',
   '33333333-3333-3333-3333-333333333333',
   'Shibuya Crossing',
   'World''s busiest pedestrian scramble crossing',
   35.6595, 139.7004,
   '2025-09-13 18:00:00+00', '2025-09-13 20:00:00+00')

ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------
-- PLACES — Trip 4: New York City  (Nov 20–27, User B + User A)
-- 9 places spread across 6 days; two stops on Nov 21, 23 and 25
-- -------------------------------------------------------------

INSERT INTO places (id, trip_id, name, description, lat, lng, start_date, end_date) VALUES

  ('d4000000-0000-0000-0000-000000000001',
   '44444444-4444-4444-4444-444444444444',
   'Central Park',
   'Iconic urban park spanning 843 acres in the heart of Manhattan',
   40.7851, -73.9683,
   '2025-11-21 10:00:00+00', '2025-11-21 13:00:00+00'),

  ('d4000000-0000-0000-0000-000000000002',
   '44444444-4444-4444-4444-444444444444',
   'Times Square',
   'Neon-lit commercial intersection and entertainment hub',
   40.7580, -73.9855,
   '2025-11-21 18:00:00+00', '2025-11-21 20:00:00+00'),

  ('d4000000-0000-0000-0000-000000000003',
   '44444444-4444-4444-4444-444444444444',
   'Museum of Modern Art',
   'World-leading collection of modern and contemporary art',
   40.7614, -73.9776,
   '2025-11-22 11:00:00+00', '2025-11-22 15:00:00+00'),

  ('d4000000-0000-0000-0000-000000000004',
   '44444444-4444-4444-4444-444444444444',
   'Brooklyn Bridge',
   'Historic suspension bridge with a pedestrian walkway above the traffic',
   40.7061, -73.9969,
   '2025-11-23 10:00:00+00', '2025-11-23 12:00:00+00'),

  ('d4000000-0000-0000-0000-000000000005',
   '44444444-4444-4444-4444-444444444444',
   'DUMBO',
   'Trendy Brooklyn neighbourhood with cobblestone streets and skyline views',
   40.7033, -73.9881,
   '2025-11-23 13:00:00+00', '2025-11-23 15:00:00+00'),

  ('d4000000-0000-0000-0000-000000000006',
   '44444444-4444-4444-4444-444444444444',
   'Metropolitan Museum of Art',
   'One of the world''s largest and most visited art museums',
   40.7794, -73.9632,
   '2025-11-24 10:00:00+00', '2025-11-24 16:00:00+00'),

  ('d4000000-0000-0000-0000-000000000007',
   '44444444-4444-4444-4444-444444444444',
   'The High Line',
   'Elevated linear park built on a former freight rail line',
   40.7480, -74.0048,
   '2025-11-25 11:00:00+00', '2025-11-25 13:00:00+00'),

  ('d4000000-0000-0000-0000-000000000008',
   '44444444-4444-4444-4444-444444444444',
   'Chelsea Market',
   'Indoor food hall and shopping mall in the Meatpacking District',
   40.7425, -74.0048,
   '2025-11-25 14:00:00+00', '2025-11-25 16:00:00+00'),

  ('d4000000-0000-0000-0000-000000000009',
   '44444444-4444-4444-4444-444444444444',
   'Statue of Liberty',
   'Colossal neoclassical sculpture on Liberty Island in New York Harbor',
   40.6892, -74.0445,
   '2025-11-26 09:00:00+00', '2025-11-26 14:00:00+00')

ON CONFLICT (id) DO NOTHING;
