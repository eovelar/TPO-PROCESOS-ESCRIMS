
const listaScrims = document.getElementById("listaScrims");
const contador = document.getElementById("contador");
const estadoCarga = document.getElementById("estadoCarga");

const busqueda = document.getElementById("busqueda");
const filtroJuego = document.getElementById("filtroJuego");
const filtroRegion = document.getElementById("filtroRegion");
const limpiarFiltros = document.getElementById("limpiarFiltros");

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

let usuarioActual = null;
let scrims = [];
let misPostulaciones = [];
let postulacionesOrganizador = [];

let procesando = false;

function crearElemento(etiqueta, clase = "", texto) {
    const elemento = document.createElement(etiqueta);

    if (clase) {
        elemento.className = clase;
    }

    if (texto !== undefined) {
        elemento.textContent = texto;
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

function detalle(etiqueta, valor) {
    const fila = crearElemento("div", "detail-row");

    fila.append(
        crearElemento("span", "", etiqueta),
        crearElemento("strong", "", String(valor))
    );

    return fila;
}

function estadoClase(estado) {
    return {
        Pendiente: "pendiente",
        Aceptada: "aceptada",
        Rechazada: "rechazada"
    }[estado] || "pendiente";
}

function estadoEtiqueta(estado) {
    return crearElemento(
        "span",
        "postulacion-estado " + estadoClase(estado),
        estado.toUpperCase()
    );
}

function notificar(titulo, mensaje, tipo = "exito") {
    let contenedor = document.getElementById("notificaciones");

    if (!contenedor) {
        contenedor = crearElemento("div", "notificaciones");
        contenedor.id = "notificaciones";
        contenedor.setAttribute("aria-live", "polite");
        document.body.append(contenedor);
    }

    const aviso = crearElemento(
        "div", "notificacion " + tipo
    );

    const icono = crearElemento(
        "div",
        "notificacion-icono",
        tipo === "exito" ? "✓" : "!"
    );

    const contenido = crearElemento(
        "div", "notificacion-contenido"
    );

    contenido.append(
        crearElemento("strong", "notificacion-titulo", titulo),
        crearElemento("p", "notificacion-mensaje", mensaje)
    );

    const cerrar = crearElemento(
        "button", "notificacion-cerrar", "×"
    );

    cerrar.type = "button";

    const progreso = crearElemento(
        "div", "notificacion-progreso"
    );

    aviso.append(icono, contenido, cerrar, progreso);
    contenedor.append(aviso);

    let cerrado = false;

    function eliminar() {
        if (cerrado) {
            return;
        }

        cerrado = true;
        aviso.classList.add("saliendo");

        setTimeout(() => aviso.remove(), 250);
    }

    cerrar.addEventListener("click", eliminar);
    setTimeout(eliminar, 5000);
}

async function solicitar(ruta, opciones = {}) {
    const respuesta = await fetch(ruta, {
        cache: "no-store",
        credentials: "same-origin",
        ...opciones
    });

    let datos;

    try {
        datos = await respuesta.json();
    } catch {
        throw new Error(
            "Respuesta inesperada del servidor"
        );
    }

    if (!respuesta.ok) {
        throw new Error(
            datos.error || "Error HTTP " + respuesta.status
        );
    }

    return datos;
}

async function enviarFormulario(ruta, datos) {
    return solicitar(ruta, {
        method: "POST",
        headers: {
            "Content-Type":
                "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams(datos).toString()
    });
}

function crearCampo(nombre, etiqueta, tipo, requerido = true) {
    const contenedor = crearElemento(
        "div", "filter-field"
    );

    const label = crearElemento("label", "", etiqueta);

    const entrada = document.createElement("input");
    entrada.name = nombre;
    entrada.type = tipo;
    entrada.required = requerido;

    if (tipo === "password") {
        entrada.minLength = 8;
    }

    contenedor.append(label, entrada);

    return contenedor;
}

function crearSeleccion(nombre, etiqueta, opciones) {
    const contenedor = crearElemento(
        "div", "filter-field"
    );

    const label = crearElemento("label", "", etiqueta);
    const select = document.createElement("select");

    select.name = nombre;
    select.required = true;

    opciones.forEach(opcion => {
        select.add(
            new Option(opcion.texto, opcion.valor)
        );
    });

    contenedor.append(label, select);

    return contenedor;
}

function crearSeccion(id, clase, titulo, descripcion) {
    const seccion = crearElemento("section", clase);
    seccion.id = id;

    const contenedor = crearElemento("div", "container");

    contenedor.append(
        crearElemento(
            "span", "section-eyebrow", "// eScrims"
        ),
        crearElemento(
            "h2", "mis-postulaciones-titulo", titulo
        ),
        crearElemento(
            "p", "section-description", descripcion
        )
    );

    seccion.append(contenedor);

    return { seccion, contenedor };
}

function crearFormularioLogin() {
    const formulario = crearElemento(
        "form", "organizador-card"
    );

    formulario.append(
        crearElemento(
            "h3", "organizador-juego", "INICIAR SESIÓN"
        ),
        crearCampo("email", "Email", "email"),
        crearCampo("password", "Contraseña", "password")
    );

    const boton = crearElemento(
        "button", "postular-button", "INGRESAR →"
    );

    boton.type = "submit";
    formulario.append(boton);

    formulario.addEventListener("submit", async evento => {
        evento.preventDefault();

        if (procesando) {
            return;
        }

        procesando = true;
        boton.disabled = true;

        try {
            const datos = Object.fromEntries(
                new FormData(formulario)
            );

            await enviarFormulario(
                "/api/auth/login", datos
            );

            await recuperarSesion();

            notificar(
                "¡Sesión iniciada!",
                "Ya podés utilizar tu cuenta de eScrims."
            );

        } catch (error) {
            notificar(
                "No se pudo ingresar",
                error.message,
                "error"
            );

        } finally {
            procesando = false;
            boton.disabled = false;
        }
    });

    return formulario;
}

function crearFormularioRegistro() {
    const formulario = crearElemento(
        "form", "organizador-card"
    );

    formulario.append(
        crearElemento(
            "h3", "organizador-juego", "CREAR CUENTA"
        ),
        crearCampo("username", "Nombre de usuario", "text"),
        crearCampo("email", "Email", "email"),
        crearCampo("password", "Contraseña", "password"),
        crearSeleccion("region", "Región", [
            { texto: "LAS", valor: "LAS" },
            { texto: "LAN", valor: "LAN" },
            { texto: "BR", valor: "BR" }
        ])
    );

    const rangos = [];

    for (let i = 1; i <= 10; i++) {
        rangos.push({
            texto: "Rango " + i,
            valor: String(i)
        });
    }

    formulario.append(
        crearSeleccion("rango", "Rango", rangos)
    );

    const boton = crearElemento(
        "button", "postular-button", "REGISTRARME →"
    );

    boton.type = "submit";
    formulario.append(boton);

    formulario.addEventListener("submit", async evento => {
        evento.preventDefault();

        if (procesando) {
            return;
        }

        procesando = true;
        boton.disabled = true;

        try {
            const datos = Object.fromEntries(
                new FormData(formulario)
            );

            await enviarFormulario(
                "/api/auth/register", datos
            );

            await recuperarSesion();

            notificar(
                "¡Cuenta creada!",
                "Tu usuario ya puede postularse a partidas."
            );

        } catch (error) {
            notificar(
                "No se pudo registrar",
                error.message,
                "error"
            );

        } finally {
            procesando = false;
            boton.disabled = false;
        }
    });

    return formulario;
}

function crearPantallaAcceso() {
    const { seccion, contenedor } = crearSeccion(
        "cuenta",
        "mis-postulaciones-section",
        "TU CUENTA",
        "Ingresá para postularte y seguir tus solicitudes."
    );

    const contenido = crearElemento(
        "div", "lista-postulaciones"
    );

    contenido.id = "contenidoCuenta";

    contenedor.append(contenido);

    document.getElementById("partidas")
        .insertAdjacentElement("afterend", seccion);

    const navegacion = document.querySelector(".navigation");

    const enlace = crearElemento(
        "a", "nav-link", "Mi cuenta"
    );

    enlace.href = "#cuenta";
    navegacion.append(enlace);

    mostrarCuenta();
}

function mostrarCuenta() {
    const contenedor = document.getElementById(
        "contenidoCuenta"
    );

    if (!usuarioActual) {
        contenedor.replaceChildren(
            crearFormularioLogin(),
            crearFormularioRegistro()
        );

        return;
    }

    const tarjeta = crearElemento(
        "div", "organizador-card"
    );

    tarjeta.append(
        crearElemento(
            "h3",
            "organizador-juego",
            usuarioActual.username
        ),
        crearElemento(
            "p",
            "organizador-jugador",
            "REGIÓN: " + usuarioActual.region +
            " · ROL: " + usuarioActual.rol
        )
    );

    const salir = crearElemento(
        "button",
        "actualizar-postulaciones",
        "CERRAR SESIÓN"
    );

    salir.type = "button";

    salir.addEventListener("click", async () => {
        try {
            await enviarFormulario(
                "/api/auth/logout", {}
            );

            await recuperarSesion();

            notificar(
                "Sesión cerrada",
                "Hasta la próxima."
            );

        } catch (error) {
            notificar(
                "Error",
                error.message,
                "error"
            );
        }
    });

    tarjeta.append(salir);

    contenedor.replaceChildren(tarjeta);
}

function crearPantallaPostulaciones() {
    const { seccion, contenedor } = crearSeccion(
        "mis-postulaciones",
        "mis-postulaciones-section",
        "MIS POSTULACIONES",
        "Consultá el estado de tus solicitudes."
    );

    const lista = crearElemento(
        "div", "lista-postulaciones"
    );

    lista.id = "listaPostulaciones";

    contenedor.append(lista);

    document.getElementById("cuenta")
        .insertAdjacentElement("afterend", seccion);

    const enlace = crearElemento(
        "a", "nav-link", "Mis postulaciones"
    );

    enlace.href = "#mis-postulaciones";

    document.querySelector(".navigation").append(enlace);
}

function crearPantallaOrganizador() {
    const { seccion, contenedor } = crearSeccion(
        "organizador",
        "organizador-section",
        "PANEL DEL ORGANIZADOR",
        "Administrá las solicitudes de los jugadores."
    );

    const resumen = crearElemento(
        "div", "organizador-resumen"
    );

    resumen.id = "resumenOrganizador";

    const lista = crearElemento(
        "div", "lista-organizador"
    );

    lista.id = "listaOrganizador";

    contenedor.append(resumen, lista);

    document.getElementById("mis-postulaciones")
        .insertAdjacentElement("afterend", seccion);

    const enlace = crearElemento(
        "a", "nav-link", "Organizador"
    );

    enlace.id = "enlaceOrganizador";
    enlace.href = "#organizador";

    document.querySelector(".navigation").append(enlace);

    seccion.hidden = true;
    enlace.hidden = true;
}

function mostrarAccesos() {
    const esOrganizador = usuarioActual &&
        usuarioActual.rol === "ORGANIZADOR";

    document.getElementById("organizador").hidden =
        !esOrganizador;

    document.getElementById(
        "enlaceOrganizador"
    ).hidden = !esOrganizador;

    mostrarCuenta();
}

async function recuperarSesion() {
    const datos = await solicitar("/api/auth/me");

    usuarioActual = datos.autenticado ? datos : null;

    mostrarAccesos();

    await actualizarTodo();
}

function crearTarjeta(scrim) {
    const tarjeta = crearElemento(
        "article", "scrim-card"
    );

    const superior = crearElemento("div", "card-top");

    superior.append(
        crearElemento(
            "div", "game-icon",
            iconosJuegos[scrim.juego] || "ES"
        ),
        crearElemento(
            "span",
            scrim.estado === "Buscando jugadores"
                ? "status"
                : "status other",
            scrim.estado === "Buscando jugadores"
                ? "BUSCANDO"
                : scrim.estado.toUpperCase()
        )
    );

    const titulo = crearElemento(
        "h4", "", nombreJuego(scrim.juego)
    );

    const subtitulo = crearElemento(
        "p", "card-subtitle",
        "Partida competitiva · " + scrim.formato
    );

    const detalles = crearElemento(
        "div", "card-details"
    );

    detalles.append(
        detalle("Región", scrim.region),
        detalle("Formato", scrim.formato),
        detalle("Fecha", formatearFecha(scrim.fecha))
    );

    const inferior = crearElemento(
        "div", "card-bottom"
    );

    const acciones = crearElemento("div", "cupos");

    acciones.append(
        crearElemento(
            "span", "cupos-texto",
            scrim.cuposDisponibles + " cupos disponibles"
        )
    );

    const boton = crearElemento(
        "button", "postular-button", "POSTULARME →"
    );

    boton.type = "button";

    const postulacion = misPostulaciones.find(
        p => p.scrimId === scrim.id
    );

    if (!usuarioActual) {
        boton.textContent = "INICIAR SESIÓN →";

        boton.addEventListener("click", () => {
            location.hash = "#cuenta";
        });

    } else if (usuarioActual.rol !== "JUGADOR") {
        boton.disabled = true;
        boton.textContent = "SOLO JUGADORES";

    } else if (postulacion) {
        boton.disabled = true;
        boton.classList.add("enviado");

        boton.textContent = postulacion.estado === "Pendiente"
            ? "POSTULACIÓN ENVIADA"
            : "SOLICITUD " + postulacion.estado.toUpperCase();

    } else if (
        scrim.estado !== "Buscando jugadores" ||
        scrim.cuposDisponibles <= 0
    ) {
        boton.disabled = true;
        boton.textContent = "NO DISPONIBLE";

    } else {
        boton.addEventListener("click", async () => {
            boton.disabled = true;
            boton.textContent = "ENVIANDO...";

            try {
                await enviarFormulario(
                    "/api/postulaciones",
                    { scrimId: scrim.id }
                );

                notificar(
                    "¡Postulación enviada!",
                    "Tu solicitud quedó pendiente."
                );

                await actualizarTodo();

            } catch (error) {
                notificar(
                    "No se pudo postular",
                    error.message,
                    "error"
                );

                boton.disabled = false;
                boton.textContent = "POSTULARME →";
            }
        });
    }

    acciones.append(boton);

    inferior.append(
        acciones,
        crearElemento(
            "span", "card-id",
            "SCRIM #" + scrim.id
        )
    );

    tarjeta.append(
        superior, titulo, subtitulo,
        detalles, inferior
    );

    return tarjeta;
}

function mostrarScrims() {
    const texto = busqueda.value.trim().toLowerCase();

    const filtrados = scrims.filter(scrim => {
        const coincideTexto = nombreJuego(scrim.juego)
            .toLowerCase().includes(texto);

        const coincideJuego =
            !filtroJuego.value ||
            filtroJuego.value === scrim.juego;

        const coincideRegion =
            !filtroRegion.value ||
            filtroRegion.value === scrim.region;

        return coincideTexto &&
            coincideJuego &&
            coincideRegion;
    });

    contador.textContent = filtrados.length + " partidas";
    estadoCarga.textContent = "Datos del servidor eScrims";

    if (filtrados.length === 0) {
        listaScrims.replaceChildren(
            crearElemento(
                "div", "message-card",
                "No encontramos partidas con esos filtros."
            )
        );
        return;
    }

    listaScrims.replaceChildren(
        ...filtrados.map(crearTarjeta)
    );
}

function actualizarFiltros() {
    const juegoAnterior = filtroJuego.value;
    const regionAnterior = filtroRegion.value;

    filtroJuego.replaceChildren(
        new Option("Todos los juegos", "")
    );

    filtroRegion.replaceChildren(
        new Option("Todas las regiones", "")
    );

    [...new Set(scrims.map(s => s.juego))]
        .sort()
        .forEach(juego => {
            filtroJuego.add(
                new Option(nombreJuego(juego), juego)
            );
        });

    [...new Set(scrims.map(s => s.region))]
        .sort()
        .forEach(region => {
            filtroRegion.add(
                new Option(region, region)
            );
        });

    filtroJuego.value = juegoAnterior;
    filtroRegion.value = regionAnterior;
}

function mostrarPostulaciones() {
    const lista = document.getElementById(
        "listaPostulaciones"
    );

    if (!usuarioActual) {
        lista.replaceChildren(
            crearElemento(
                "div", "message-card",
                "Iniciá sesión para consultar tus postulaciones."
            )
        );
        return;
    }

    if (misPostulaciones.length === 0) {
        lista.replaceChildren(
            crearElemento(
                "div", "message-card",
                "Todavía no tenés postulaciones."
            )
        );
        return;
    }

    lista.replaceChildren(
        ...misPostulaciones.map(p => {
            const tarjeta = crearElemento(
                "article", "postulacion-card"
            );

            const cabecera = crearElemento(
                "div", "postulacion-cabecera"
            );

            const informacion = crearElemento("div");

            informacion.append(
                crearElemento(
                    "span", "postulacion-id",
                    "SOLICITUD #" + p.id
                ),
                crearElemento(
                    "h3", "postulacion-juego",
                    nombreJuego(p.juego)
                )
            );

            cabecera.append(
                informacion,
                estadoEtiqueta(p.estado)
            );

            const detalles = crearElemento(
                "div", "postulacion-detalles"
            );

            detalles.append(
                detalle("Partida", "#" + p.scrimId),
                detalle("Formato", p.formato),
                detalle("Región", p.region),
                detalle("Fecha", formatearFecha(p.fecha))
            );

            tarjeta.append(
                cabecera,
                detalles,
                crearElemento(
                    "p", "postulacion-mensaje",
                    p.estado === "Pendiente"
                        ? "Esperando aprobación del organizador."
                        : "Solicitud " +
                        p.estado.toLowerCase() + "."
                )
            );

            return tarjeta;
        })
    );
}

function resumenOrganizador() {
    const contenedor = document.getElementById(
        "resumenOrganizador"
    );

    const estados = [
        "Pendiente",
        "Aceptada",
        "Rechazada"
    ];

    contenedor.replaceChildren(
        ...estados.map(estado => {
            const tarjeta = crearElemento(
                "div", "organizador-resumen-item"
            );

            const cantidad = postulacionesOrganizador.filter(
                p => p.estado === estado
            ).length;

            tarjeta.append(
                crearElemento(
                    "strong", "", String(cantidad)
                ),
                crearElemento(
                    "span", "", estado + "s"
                )
            );

            return tarjeta;
        })
    );
}

function mostrarOrganizador() {
    const lista = document.getElementById(
        "listaOrganizador"
    );

    resumenOrganizador();

    if (postulacionesOrganizador.length === 0) {
        lista.replaceChildren(
            crearElemento(
                "div", "message-card",
                "No hay solicitudes para revisar."
            )
        );
        return;
    }

    lista.replaceChildren(
        ...postulacionesOrganizador.map(p => {
            const tarjeta = crearElemento(
                "article", "organizador-card"
            );

            const cabecera = crearElemento(
                "div", "organizador-card-cabecera"
            );

            const informacion = crearElemento("div");

            informacion.append(
                crearElemento(
                    "span", "postulacion-id",
                    "SOLICITUD #" + p.id
                ),
                crearElemento(
                    "h3", "organizador-juego",
                    nombreJuego(p.juego)
                ),
                crearElemento(
                    "p", "organizador-jugador",
                    p.usuario + " · SCRIM #" + p.scrimId
                )
            );

            cabecera.append(
                informacion,
                estadoEtiqueta(p.estado)
            );

            const detalles = crearElemento(
                "div", "organizador-card-detalles"
            );

            detalles.append(
                detalle("Formato", p.formato),
                detalle("Región", p.region),
                detalle("Fecha", formatearFecha(p.fecha))
            );

            tarjeta.append(cabecera, detalles);

            if (p.estado === "Pendiente") {
                const acciones = crearElemento(
                    "div", "organizador-card-acciones"
                );

                for (const accion of ["aceptar", "rechazar"]) {
                    const boton = crearElemento(
                        "button",
                        "organizador-" + accion,
                        accion.toUpperCase()
                    );

                    boton.type = "button";

                    boton.addEventListener(
                        "click", async () => {
                            if (procesando) {
                                return;
                            }

                            procesando = true;
                            boton.disabled = true;

                            try {
                                await enviarFormulario(
                                    "/api/organizador/postulaciones",
                                    {
                                        postulacionId: p.id,
                                        accion
                                    }
                                );

                                notificar(
                                    "Solicitud procesada",
                                    "El estado fue actualizado."
                                );

                                await actualizarTodo();

                            } catch (error) {
                                notificar(
                                    "Error",
                                    error.message,
                                    "error"
                                );

                            } finally {
                                procesando = false;
                                boton.disabled = false;
                            }
                        }
                    );

                    acciones.append(boton);
                }

                tarjeta.append(acciones);
            }

            lista.append(tarjeta);
            return tarjeta;
        })
    );
}

async function actualizarTodo() {
    try {
        scrims = await solicitar("/api/scrims");

        misPostulaciones = [];
        postulacionesOrganizador = [];

        if (usuarioActual) {
            misPostulaciones = await solicitar(
                "/api/postulaciones"
            );

            if (usuarioActual.rol === "ORGANIZADOR") {
                postulacionesOrganizador = await solicitar(
                    "/api/organizador/postulaciones"
                );
            }
        }

        actualizarFiltros();
        mostrarScrims();
        mostrarPostulaciones();
        mostrarOrganizador();

    } catch (error) {
        console.error(error);

        notificar(
            "Error de conexión",
            error.message,
            "error"
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

async function iniciarAplicacion() {
    crearPantallaAcceso();
    crearPantallaPostulaciones();
    crearPantallaOrganizador();

    try {
        await recuperarSesion();
    } catch (error) {
        console.error(error);

        notificar(
            "No se pudo conectar",
            "Comprobá que el servidor Java esté funcionando.",
            "error"
        );
    }
}

iniciarAplicacion();
