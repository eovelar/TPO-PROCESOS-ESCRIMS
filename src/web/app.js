
const listaScrims = document.getElementById("listaScrims");
const contador = document.getElementById("contador");
const estadoCarga = document.getElementById("estadoCarga");

const busqueda = document.getElementById("busqueda");
const filtroJuego = document.getElementById("filtroJuego");
const filtroRegion = document.getElementById("filtroRegion");
const limpiarFiltros = document.getElementById("limpiarFiltros");

let scrims = [];
let misPostulaciones = [];
let postulacionesOrganizador = [];

let postulacionesCargadas = false;
let procesandoSolicitud = false;

const postulacionesEnProceso = new Set();

const nombresJuegos = {
    Valorant: "VALORANT",
    LoL: "LEAGUE OF LEGENDS",
    CS2: "COUNTER-STRIKE 2"
};

const iconosJuegos = {
    Valorant: "VAL",
    LoL: "LOL",
    CS2: "CS2"
};

function crearElemento(etiqueta, clase = "", contenido) {
    const elemento = document.createElement(etiqueta);

    if (clase) {
        elemento.className = clase;
    }

    if (contenido !== undefined) {
        elemento.textContent = contenido;
    }

    return elemento;
}

function nombreJuego(juego) {
    return nombresJuegos[juego] || juego;
}

function formatearFecha(fecha) {
    if (!fecha) {
        return "Sin fecha";
    }

    const valor = new Date(fecha);

    if (Number.isNaN(valor.getTime())) {
        return fecha;
    }

    return new Intl.DateTimeFormat("es-AR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(valor);
}

function crearDetalle(nombre, valor) {
    const fila = crearElemento("div", "detail-row");

    fila.append(
        crearElemento("span", "", nombre),
        crearElemento("strong", "", String(valor))
    );

    return fila;
}

function claseEstado(estado) {
    switch (estado) {
        case "Aceptada":
            return "aceptada";
        case "Rechazada":
            return "rechazada";
        default:
            return "pendiente";
    }
}

function crearEstado(estado, claseBase) {
    return crearElemento(
        "span",
        claseBase + " " + claseEstado(estado),
        estado.toUpperCase()
    );
}

function mostrarNotificacion(titulo, mensaje, tipo = "exito") {
    let contenedor = document.getElementById("notificaciones");

    if (!contenedor) {
        contenedor = crearElemento("div", "notificaciones");
        contenedor.id = "notificaciones";
        contenedor.setAttribute("aria-live", "polite");
        document.body.appendChild(contenedor);
    }

    const notificacion = crearElemento(
        "div",
        "notificacion " + tipo
    );

    const icono = crearElemento(
        "div",
        "notificacion-icono",
        tipo === "exito" ? "✓" : "!"
    );

    const contenido = crearElemento(
        "div",
        "notificacion-contenido"
    );

    contenido.append(
        crearElemento("strong", "notificacion-titulo", titulo),
        crearElemento("p", "notificacion-mensaje", mensaje)
    );

    const cerrar = crearElemento(
        "button",
        "notificacion-cerrar",
        "×"
    );

    cerrar.type = "button";
    cerrar.setAttribute("aria-label", "Cerrar notificación");

    const progreso = crearElemento(
        "div",
        "notificacion-progreso"
    );

    let temporizador;
    let cerrando = false;

    function quitarNotificacion() {
        if (cerrando) {
            return;
        }

        cerrando = true;
        clearTimeout(temporizador);
        notificacion.classList.add("saliendo");

        setTimeout(() => {
            notificacion.remove();
        }, 250);
    }

    cerrar.addEventListener("click", quitarNotificacion);

    notificacion.append(
        icono,
        contenido,
        cerrar,
        progreso
    );

    contenedor.appendChild(notificacion);

    temporizador = setTimeout(quitarNotificacion, 5000);
}

function crearEnlaceMenu(texto, destino) {
    const navegacion = document.querySelector(".navigation");

    if (!navegacion || navegacion.querySelector(
        'a[href="' + destino + '"]'
    )) {
        return;
    }

    const enlace = crearElemento("a", "nav-link", texto);
    enlace.href = destino;
    navegacion.appendChild(enlace);
}

function crearSecciones() {
    const explorar = document.getElementById("partidas");

    const seccionJugador = crearElemento(
        "section",
        "mis-postulaciones-section"
    );

    seccionJugador.id = "mis-postulaciones";

    const contenedorJugador = crearElemento("div", "container");

    const encabezadoJugador = crearElemento(
        "div",
        "mis-postulaciones-encabezado"
    );

    const titulosJugador = crearElemento("div");

    titulosJugador.append(
        crearElemento("span", "section-eyebrow", "// Mi actividad"),
        crearElemento(
            "h2",
            "mis-postulaciones-titulo",
            "Mis postulaciones"
        ),
        crearElemento(
            "p",
            "section-description",
            "Consultá el estado de tus solicitudes."
        )
    );

    const accionesJugador = crearElemento(
        "div",
        "mis-postulaciones-acciones"
    );

    const contadorJugador = crearElemento(
        "span",
        "results-badge",
        "0 solicitudes"
    );

    contadorJugador.id = "contadorPostulaciones";

    const actualizarJugador = crearElemento(
        "button",
        "actualizar-postulaciones",
        "ACTUALIZAR ↻"
    );

    actualizarJugador.type = "button";
    actualizarJugador.addEventListener(
        "click",
        actualizarTodo
    );

    accionesJugador.append(contadorJugador, actualizarJugador);
    encabezadoJugador.append(titulosJugador, accionesJugador);

    const listaJugador = crearElemento(
        "div",
        "lista-postulaciones"
    );

    listaJugador.id = "listaPostulaciones";
    listaJugador.setAttribute("aria-live", "polite");

    contenedorJugador.append(encabezadoJugador, listaJugador);
    seccionJugador.appendChild(contenedorJugador);

    explorar.insertAdjacentElement(
        "afterend",
        seccionJugador
    );

    const seccionOrganizador = crearElemento(
        "section",
        "organizador-section"
    );

    seccionOrganizador.id = "organizador";

    const contenedorOrganizador = crearElemento(
        "div",
        "container"
    );

    const encabezadoOrganizador = crearElemento(
        "div",
        "organizador-encabezado"
    );

    const titulosOrganizador = crearElemento("div");

    titulosOrganizador.append(
        crearElemento(
            "span",
            "section-eyebrow",
            "// Gestión de partidas"
        ),
        crearElemento(
            "h2",
            "organizador-titulo",
            "Panel del organizador"
        ),
        crearElemento(
            "p",
            "section-description",
            "Revisá las solicitudes y administrá los participantes."
        )
    );

    const etiquetaDemo = crearElemento(
        "span",
        "organizador-demo",
        "MODO DEMO"
    );

    encabezadoOrganizador.append(
        titulosOrganizador,
        etiquetaDemo
    );

    const resumen = crearElemento(
        "div",
        "organizador-resumen"
    );

    resumen.id = "resumenOrganizador";

    const barra = crearElemento(
        "div",
        "organizador-barra"
    );

    const subtitulo = crearElemento(
        "h3",
        "",
        "Solicitudes recibidas"
    );

    const actualizarOrganizador = crearElemento(
        "button",
        "actualizar-postulaciones",
        "ACTUALIZAR ↻"
    );

    actualizarOrganizador.type = "button";
    actualizarOrganizador.addEventListener(
        "click",
        actualizarTodo
    );

    barra.append(subtitulo, actualizarOrganizador);

    const listaOrganizador = crearElemento(
        "div",
        "lista-organizador"
    );

    listaOrganizador.id = "listaOrganizador";
    listaOrganizador.setAttribute("aria-live", "polite");

    contenedorOrganizador.append(
        encabezadoOrganizador,
        resumen,
        barra,
        listaOrganizador
    );

    seccionOrganizador.appendChild(
        contenedorOrganizador
    );

    seccionJugador.insertAdjacentElement(
        "afterend",
        seccionOrganizador
    );

    crearEnlaceMenu(
        "Mis postulaciones",
        "#mis-postulaciones"
    );

    crearEnlaceMenu(
        "Organizador",
        "#organizador"
    );
}

function crearTarjetaPostulacion(postulacion) {
    const tarjeta = crearElemento(
        "article",
        "postulacion-card"
    );

    const cabecera = crearElemento(
        "div",
        "postulacion-cabecera"
    );

    const informacion = crearElemento("div");

    informacion.append(
        crearElemento(
            "span",
            "postulacion-id",
            "SOLICITUD #" + postulacion.id
        ),
        crearElemento(
            "h3",
            "postulacion-juego",
            nombreJuego(postulacion.juego)
        )
    );

    cabecera.append(
        informacion,
        crearEstado(postulacion.estado, "postulacion-estado")
    );

    const detalles = crearElemento(
        "div",
        "postulacion-detalles"
    );

    detalles.append(
        crearDetalle("Partida", "#" + postulacion.scrimId),
        crearDetalle("Formato", postulacion.formato),
        crearDetalle("Región", postulacion.region),
        crearDetalle("Fecha", formatearFecha(postulacion.fecha))
    );

    let mensaje;

    switch (postulacion.estado) {
        case "Aceptada":
            mensaje = "Tu postulación fue aceptada.";
            break;
        case "Rechazada":
            mensaje = "Tu postulación fue rechazada.";
            break;
        default:
            mensaje = "Esperando la aprobación del organizador.";
    }

    tarjeta.append(
        cabecera,
        detalles,
        crearElemento(
            "p",
            "postulacion-mensaje",
            mensaje
        )
    );

    return tarjeta;
}

function mostrarPostulaciones() {
    const lista = document.getElementById(
        "listaPostulaciones"
    );

    const contadorJugador = document.getElementById(
        "contadorPostulaciones"
    );

    contadorJugador.textContent =
        misPostulaciones.length + " solicitudes";

    if (misPostulaciones.length === 0) {
        lista.replaceChildren(
            crearElemento(
                "div",
                "message-card",
                "Todavía no tenés postulaciones registradas."
            )
        );
        return;
    }

    lista.replaceChildren(
        ...misPostulaciones.map(crearTarjetaPostulacion)
    );
}

function crearResumen(numero, texto) {
    const tarjeta = crearElemento(
        "div",
        "organizador-resumen-item"
    );

    tarjeta.append(
        crearElemento(
            "strong",
            "",
            String(numero)
        ),
        crearElemento(
            "span",
            "",
            texto
        )
    );

    return tarjeta;
}

function mostrarResumenOrganizador() {
    const pendientes = postulacionesOrganizador.filter(
        p => p.estado === "Pendiente"
    ).length;

    const aceptadas = postulacionesOrganizador.filter(
        p => p.estado === "Aceptada"
    ).length;

    const rechazadas = postulacionesOrganizador.filter(
        p => p.estado === "Rechazada"
    ).length;

    document.getElementById(
        "resumenOrganizador"
    ).replaceChildren(
        crearResumen(pendientes, "Pendientes"),
        crearResumen(aceptadas, "Aceptadas"),
        crearResumen(rechazadas, "Rechazadas")
    );
}

async function gestionarPostulacion(postulacion, accion) {
    if (procesandoSolicitud || postulacion.estado !== "Pendiente") {
        return;
    }

    procesandoSolicitud = true;
    mostrarOrganizador();

    try {
        const parametros = new URLSearchParams();

        parametros.set(
            "postulacionId",
            String(postulacion.id)
        );

        parametros.set("accion", accion);

        const respuesta = await fetch(
            "/api/organizador/postulaciones",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },
                body: parametros.toString()
            }
        );

        const datos = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(
                datos.error || "No se pudo procesar la solicitud"
            );
        }

        mostrarNotificacion(
            accion === "aceptar"
                ? "¡Jugador aceptado!"
                : "Solicitud rechazada",
            "La solicitud #" + postulacion.id +
            " ahora está " + datos.estado.toLowerCase() + "."
        );

        await actualizarTodo();

    } catch (error) {
        mostrarNotificacion(
            "No se pudo completar la acción",
            error.message,
            "error"
        );

    } finally {
        procesandoSolicitud = false;
        mostrarOrganizador();
    }
}

function crearTarjetaOrganizador(postulacion) {
    const tarjeta = crearElemento(
        "article",
        "organizador-card"
    );

    const cabecera = crearElemento(
        "div",
        "organizador-card-cabecera"
    );

    const informacion = crearElemento("div");

    informacion.append(
        crearElemento(
            "span",
            "postulacion-id",
            "SOLICITUD #" + postulacion.id
        ),
        crearElemento(
            "h3",
            "organizador-juego",
            nombreJuego(postulacion.juego)
        ),
        crearElemento(
            "p",
            "organizador-jugador",
            postulacion.usuario + " · SCRIM #" +
            postulacion.scrimId
        )
    );

    cabecera.append(
        informacion,
        crearEstado(
            postulacion.estado,
            "postulacion-estado"
        )
    );

    const detalles = crearElemento(
        "div",
        "organizador-card-detalles"
    );

    detalles.append(
        crearDetalle("Formato", postulacion.formato),
        crearDetalle("Región", postulacion.region),
        crearDetalle("Fecha", formatearFecha(postulacion.fecha))
    );

    tarjeta.append(cabecera, detalles);

    if (postulacion.estado === "Pendiente") {
        const acciones = crearElemento(
            "div",
            "organizador-card-acciones"
        );

        const aceptar = crearElemento(
            "button",
            "organizador-aceptar",
            "✓ ACEPTAR"
        );

        const rechazar = crearElemento(
            "button",
            "organizador-rechazar",
            "× RECHAZAR"
        );

        aceptar.type = "button";
        rechazar.type = "button";

        aceptar.disabled = procesandoSolicitud;
        rechazar.disabled = procesandoSolicitud;

        aceptar.addEventListener("click", () => {
            gestionarPostulacion(postulacion, "aceptar");
        });

        rechazar.addEventListener("click", () => {
            gestionarPostulacion(postulacion, "rechazar");
        });

        acciones.append(aceptar, rechazar);
        tarjeta.appendChild(acciones);

    } else {
        tarjeta.append(
            crearElemento(
                "p",
                "organizador-resuelto",
                "Solicitud " +
                postulacion.estado.toLowerCase() +
                ". No hay acciones pendientes."
            )
        );
    }

    return tarjeta;
}

function mostrarOrganizador() {
    const lista = document.getElementById(
        "listaOrganizador"
    );

    mostrarResumenOrganizador();

    if (postulacionesOrganizador.length === 0) {
        lista.replaceChildren(
            crearElemento(
                "div",
                "message-card",
                "Todavía no hay solicitudes para revisar."
            )
        );
        return;
    }

    const ordenadas = [...postulacionesOrganizador].sort(
        (a, b) => {
            if (a.estado === "Pendiente" &&
                b.estado !== "Pendiente") {
                return -1;
            }

            if (b.estado === "Pendiente" &&
                a.estado !== "Pendiente") {
                return 1;
            }

            return a.id - b.id;
        }
    );

    lista.replaceChildren(
        ...ordenadas.map(crearTarjetaOrganizador)
    );
}

async function enviarPostulacion(scrim, boton) {
    if (postulacionesEnProceso.has(scrim.id)) {
        return;
    }

    postulacionesEnProceso.add(scrim.id);
    boton.disabled = true;
    boton.textContent = "ENVIANDO...";

    try {
        const parametros = new URLSearchParams();
        parametros.set("scrimId", String(scrim.id));

        const respuesta = await fetch(
            "/api/postulaciones",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },
                body: parametros.toString()
            }
        );

        const datos = await respuesta.json();

        if (!respuesta.ok) {
            throw new Error(
                datos.error || "No se pudo registrar la postulación"
            );
        }

        mostrarNotificacion(
            "¡Postulación enviada!",
            "Tu solicitud para " + nombreJuego(scrim.juego) +
            " quedó pendiente de aceptación."
        );

        await actualizarTodo();

    } catch (error) {
        mostrarNotificacion(
            "No se pudo enviar",
            error.message,
            "error"
        );

    } finally {
        postulacionesEnProceso.delete(scrim.id);
        mostrarScrims();
    }
}

function crearTarjeta(scrim) {
    const tarjeta = crearElemento(
        "article",
        "scrim-card"
    );

    const superior = crearElemento("div", "card-top");

    const icono = crearElemento(
        "div",
        "game-icon",
        iconosJuegos[scrim.juego] || "ES"
    );

    const buscando =
        scrim.estado === "Buscando jugadores";

    const estado = crearElemento(
        "span",
        buscando ? "status" : "status other",
        buscando
            ? "BUSCANDO"
            : scrim.estado.toUpperCase()
    );

    superior.append(icono, estado);

    const titulo = crearElemento(
        "h4",
        "",
        nombreJuego(scrim.juego)
    );

    const subtitulo = crearElemento(
        "p",
        "card-subtitle",
        "Partida competitiva · " + scrim.formato
    );

    const detalles = crearElemento(
        "div",
        "card-details"
    );

    detalles.append(
        crearDetalle("Región", scrim.region),
        crearDetalle("Formato", scrim.formato),
        crearDetalle("Fecha", formatearFecha(scrim.fecha))
    );

    const inferior = crearElemento(
        "div",
        "card-bottom"
    );

    const acciones = crearElemento("div", "cupos");

    const cantidadCupos = crearElemento(
        "span",
        "cupos-texto",
        scrim.cuposDisponibles + " cupos disponibles"
    );

    const boton = crearElemento(
        "button",
        "postular-button",
        "POSTULARME →"
    );

    boton.type = "button";

    const postulacion = misPostulaciones.find(
        p => p.scrimId === scrim.id
    );

    const disponible =
        buscando && scrim.cuposDisponibles > 0;

    if (!postulacionesCargadas) {
        boton.disabled = true;
        boton.textContent = "CARGANDO...";

    } else if (postulacion) {
        boton.disabled = true;
        boton.classList.add("enviado");

        boton.textContent =
            postulacion.estado === "Pendiente"
                ? "POSTULACIÓN ENVIADA"
                : "SOLICITUD " +
                postulacion.estado.toUpperCase();

    } else if (!disponible) {
        boton.disabled = true;
        boton.textContent = "NO DISPONIBLE";

    } else if (postulacionesEnProceso.has(scrim.id)) {
        boton.disabled = true;
        boton.textContent = "ENVIANDO...";

    } else {
        boton.addEventListener("click", () => {
            enviarPostulacion(scrim, boton);
        });
    }

    acciones.append(cantidadCupos, boton);

    const identificador = crearElemento(
        "span",
        "card-id",
        "SCRIM #" + scrim.id
    );

    inferior.append(acciones, identificador);

    tarjeta.append(
        superior,
        titulo,
        subtitulo,
        detalles,
        inferior
    );

    return tarjeta;
}

function actualizarFiltros() {
    const juegos = [
        ...new Set(scrims.map(scrim => scrim.juego))
    ].sort();

    const regiones = [
        ...new Set(scrims.map(scrim => scrim.region))
    ].sort();

    const juegoAnterior = filtroJuego.value;
    const regionAnterior = filtroRegion.value;

    filtroJuego.replaceChildren(
        new Option("Todos los juegos", "")
    );

    filtroRegion.replaceChildren(
        new Option("Todas las regiones", "")
    );

    juegos.forEach(juego => {
        filtroJuego.add(
            new Option(nombreJuego(juego), juego)
        );
    });

    regiones.forEach(region => {
        filtroRegion.add(new Option(region, region));
    });

    filtroJuego.value = juegoAnterior;
    filtroRegion.value = regionAnterior;
}

function mostrarScrims() {
    const texto = busqueda.value.trim().toLowerCase();
    const juego = filtroJuego.value;
    const region = filtroRegion.value;

    const filtrados = scrims.filter(scrim => {
        const nombre = nombreJuego(scrim.juego);

        const coincideTexto =
            nombre.toLowerCase().includes(texto) ||
            scrim.juego.toLowerCase().includes(texto);

        const coincideJuego =
            !juego || scrim.juego === juego;

        const coincideRegion =
            !region || scrim.region === region;

        return (
            coincideTexto &&
            coincideJuego &&
            coincideRegion
        );
    });

    contador.textContent =
        filtrados.length + " partidas";

    estadoCarga.textContent =
        "Datos del servidor eScrims";

    if (filtrados.length === 0) {
        listaScrims.replaceChildren(
            crearElemento(
                "div",
                "message-card",
                "No encontramos partidas con esos filtros."
            )
        );
        return;
    }

    listaScrims.replaceChildren(
        ...filtrados.map(crearTarjeta)
    );
}

async function obtenerJson(ruta) {
    const respuesta = await fetch(ruta, {
        method: "GET",
        cache: "no-store"
    });

    if (!respuesta.ok) {
        throw new Error(
            "Error HTTP " + respuesta.status +
            " al consultar " + ruta
        );
    }

    const datos = await respuesta.json();

    if (!Array.isArray(datos)) {
        throw new Error(
            "Respuesta inesperada del servidor"
        );
    }

    return datos;
}

async function actualizarTodo() {
    try {
        const datos = await Promise.all([
            obtenerJson("/api/scrims"),
            obtenerJson("/api/postulaciones"),
            obtenerJson("/api/organizador/postulaciones")
        ]);

        scrims = datos[0];
        misPostulaciones = datos[1];
        postulacionesOrganizador = datos[2];
        postulacionesCargadas = true;

        actualizarFiltros();
        mostrarScrims();
        mostrarPostulaciones();
        mostrarOrganizador();

    } catch (error) {
        console.error(
            "Error al actualizar eScrims:",
            error
        );

        postulacionesCargadas = false;

        mostrarNotificacion(
            "Error de conexión",
            "No se pudieron actualizar los datos. " +
            "Comprobá que Java esté ejecutándose.",
            "error"
        );

        estadoCarga.textContent =
            "Error de conexión";

        listaScrims.replaceChildren(
            crearElemento(
                "div",
                "message-card",
                "No se pudieron cargar las partidas."
            )
        );

        document.getElementById(
            "listaPostulaciones"
        ).replaceChildren(
            crearElemento(
                "div",
                "message-card",
                "No se pudieron cargar las postulaciones."
            )
        );

        document.getElementById(
            "listaOrganizador"
        ).replaceChildren(
            crearElemento(
                "div",
                "message-card",
                "No se pudieron cargar las solicitudes."
            )
        );
    }
}

busqueda.addEventListener("input", mostrarScrims);
filtroJuego.addEventListener("change", mostrarScrims);
filtroRegion.addEventListener("change", mostrarScrims);

limpiarFiltros.addEventListener("click", () => {
    busqueda.value = "";
    filtroJuego.value = "";
    filtroRegion.value = "";

    mostrarScrims();
});

crearSecciones();
actualizarTodo();
