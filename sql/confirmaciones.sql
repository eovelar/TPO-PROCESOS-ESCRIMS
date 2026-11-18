-- Confirmaciones individuales del lobby (una por jugador y partida).
-- Ejecutar una sola vez en el SQL Editor de Supabase.
CREATE TABLE IF NOT EXISTS public.confirmaciones (
    scrim_id      integer     NOT NULL REFERENCES public.scrims(id)   ON DELETE CASCADE,
    usuario_id    integer     NOT NULL REFERENCES public.usuarios(id) ON DELETE CASCADE,
    confirmado_en timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (scrim_id, usuario_id)
);

-- Log de auditoría de cambios de estado (requisito no funcional).
-- Registra cada transición con quién la produjo y cuándo.
CREATE TABLE IF NOT EXISTS public.auditoria_estados (
    id              serial      PRIMARY KEY,
    scrim_id        integer     NOT NULL REFERENCES public.scrims(id) ON DELETE CASCADE,
    estado_anterior text        NOT NULL,
    estado_nuevo    text        NOT NULL,
    origen          text        NOT NULL DEFAULT 'sistema',
    creado_en       timestamptz NOT NULL DEFAULT now()
);
