-- El nombre artistico se busca sin distinguir mayusculas (ArtistRepository
-- .findByStageNameIgnoreCase y las consultas de eventos por artista). La
-- restriccion UNIQUE de V1 distingue mayusculas, por lo que permitiria
-- "Solar Beat" y "solar beat" a la vez y la busqueda devolveria dos filas.
-- Este indice hace que la base de datos aplique la misma regla que la
-- aplicacion, igual que ux_users_email_lower para el correo.
CREATE UNIQUE INDEX ux_artists_stage_name_lower ON artists (LOWER(stage_name));
