/* ==========================================================
   eScrims — app.js
   Partidas, cuenta, postulaciones, panel organizador,
   notificaciones e indicador de sección.
   ========================================================== */

/* marca que hay JS: habilita las animaciones que dependen de él */
document.documentElement.classList.add("js");

/* elementos */

const listaScrims = document.getElementById("listaScrims");
const contador = document.getElementById("contador");
const estadoCarga = document.getElementById("estadoCarga");

const busqueda = document.getElementById("busqueda");
const filtroJuego = document.getElementById("filtroJuego");
const filtroRegion = document.getElementById("filtroRegion");
const limpiarFiltros = document.getElementById("limpiarFiltros");

const formLogin = document.getElementById("formLogin");
const formRegistro = document.getElementById("formRegistro");
const contenidoCuenta = document.getElementById("contenidoCuenta");
const cuentaDescripcion = document.getElementById("cuentaDescripcion");
const perfilCuenta = document.getElementById("perfilCuenta");
const cerrarSesionBoton = document.getElementById("cerrarSesion");

const seccionPostulaciones = document.getElementById("mis-postulaciones");
const listaPostulaciones = document.getElementById("listaPostulaciones");
const actualizarPostulacionesBoton = document.getElementById("actualizarPostulaciones");

const seccionOrganizador = document.getElementById("organizador");
const listaOrganizador = document.getElementById("listaOrganizador");
const actualizarOrganizadorBoton = document.getElementById("actualizarOrganizador");

const navPostulaciones = document.getElementById("navPostulaciones");
const navOrganizador = document.getElementById("navOrganizador");

/* estado */

let scrims = [];
let scrimsCargadas = false;
let usuario = null;
let postulaciones = [];
let modoDemo = false;

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

/* utilidades */

function crearElemento(etiqueta, clase, contenido) {
    const elemento = document.createElement(etiqueta);

    if (clase) {
        elemento.className = clase;
    }

    if (contenido !== undefined) {
        elemento.textContent = contenido;
    }

    return elemento;
}

function formatearFecha(fecha) {
    if (!fecha) {
        return "Sin fecha";
    }

    // LocalDateTime de Java puede traer nanosegundos: se recortan
    const valor = new Date(String(fecha).replace(/\.\d+$/, ""));

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
    const etiqueta = crearElemento("span", "", nombre);
    const contenido = crearElemento("strong", "", String(valor));

    fila.append(etiqueta, contenido);

    return fila;
}

function nombreJuego(juego) {
    return nombresJuegos[juego] || juego;
}

function claseEstado(estado) {
    const texto = String(estado || "").toLowerCase();

    if (texto.startsWith("acept")) {
        return "aceptada";
    }

    if (texto.startsWith("rechaz")) {
        return "rechazada";
    }

    return "pendiente";
}

function irA(id) {
    const destino = document.getElementById(id);

    if (destino) {
        destino.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

/* comunicación con el servidor */

async function api(url, opciones = {}) {
    const respuesta = await fetch(url, {
        cache: "no-store",
        ...opciones
    });

    let datos = null;

    try {
        datos = await respuesta.json();
    } catch (error) {
        datos = null;
    }

    if (!respuesta.ok) {
        const error = new Error(
            (datos && datos.error) || "Error HTTP " + respuesta.status
        );
        error.status = respuesta.status;
        throw error;
    }

    return datos;
}

function enviarFormulario(url, valores) {
    return api(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams(valores)
    });
}

/* ==========================================================
   mascota pixelada (16x16, dibujada con SVG)
   ========================================================== */

const COLORES_BICHO = {
    o: "#4a2fa8", // contorno
    v: "#a07cff", // cuerpo
    w: "#d9ccff", // brillo
    l: "#d6ff5e", // ojos y antenas
    k: "#120d1f", // pupilas y boca
    p: "#ff8fc0"  // cachetes
};

const ANTENAS_BICHO = [
    "....l......l....",
    ".....o....o.....",
    "......o..o......"
];

const CUERPO_BICHO = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...ovwwvvvvvo...",
    "..ovwvvvvvvvvo..",
    "..ovvvvvvvvvvo..",
    "..ovvvvvvvvvvo..",
    "..ovvvvvvvvvvo..",
    "..ovvvvvvvvvvo..",
    ".oovvvvvvvvvvoo.",
    "o.ovvvvvvvvvvo.o",
    "...ovvvvvvvvo...",
    "....oooooooo....",
    "....o.o..o.o....",
    "................"
];

const CARAS_BICHO = {
    feliz: {
        abiertos: [[6, ".....ll..ll....."], [7, ".....lk..lk....."]],
        cerrados: [[7, ".....kk..kk....."]],
        boca: [[9, "...pp.k..k.pp..."], [10, ".......kk......."]]
    },
    triste: {
        abiertos: [[7, ".....l....l....."], [8, ".....k....k....."]],
        cerrados: [[8, ".....k....k....."]],
        boca: [[10, ".......kk......."], [11, "......k..k......"]]
    }
};

function pixeles(filas) {
    let html = "";

    filas.forEach(([y, fila]) => {
        [...fila].forEach((letra, x) => {
            if (COLORES_BICHO[letra]) {
                html += '<rect x="' + x + '" y="' + y +
                    '" width="1" height="1" fill="' + COLORES_BICHO[letra] + '"/>';
            }
        });
    });

    return html;
}

// tipo: "feliz", "triste" o "cargando"
function crearBicho(tipo = "feliz") {
    const cara = CARAS_BICHO[tipo === "triste" ? "triste" : "feliz"];
    const numeradas = filas => filas.map((fila, y) => [y, fila]);

    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("viewBox", "0 0 16 16");
    svg.setAttribute("shape-rendering", "crispEdges");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("class", "bicho" + (tipo === "cargando" ? " saltando" : ""));

    svg.innerHTML =
        '<g class="bicho-cuerpo">' +
        '<g class="bicho-antenas">' + pixeles(numeradas(ANTENAS_BICHO)) + "</g>" +
        pixeles(numeradas(CUERPO_BICHO)) +
        '<g class="bicho-abiertos">' + pixeles(cara.abiertos) + "</g>" +
        '<g class="bicho-cerrados">' + pixeles(cara.cerrados) + "</g>" +
        pixeles(cara.boca) +
        "</g>";

    return svg;
}

/* ==========================================================
   avatar pixel: 5x5 simétrico generado a partir del usuario.
   El mismo nombre siempre da el mismo dibujo.
   ========================================================== */

function hashTexto(texto) {
    // FNV-1a de 32 bits
    let hash = 2166136261;

    for (const caracter of String(texto)) {
        hash ^= caracter.codePointAt(0);
        hash = Math.imul(hash, 16777619) >>> 0;
    }

    return hash;
}

function crearAvatar(nombre) {
    const hash = hashTexto(String(nombre).toLowerCase());
    const celdas = [];

    // 3 columnas x 5 filas = 15 bits; las columnas 3 y 4 son espejo
    for (let y = 0; y < 5; y++) {
        for (let x = 0; x < 3; x++) {
            const bit = y * 3 + x;

            if ((hash >>> bit) & 1) {
                // algunas celdas en lima, el resto violeta
                const lima = ((hash >>> (15 + ((bit + y) % 16))) & 3) === 0;
                celdas.push({ x, y, lima });
            }
        }
    }

    // que nunca quede casi vacío
    if (celdas.length < 5) {
        celdas.push({ x: 2, y: 1, lima: false }, { x: 2, y: 2, lima: true }, { x: 2, y: 3, lima: false });
    }

    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("viewBox", "-1 -1 7 7");
    svg.setAttribute("shape-rendering", "crispEdges");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("class", "avatar-pixel");

    let html = '<rect x="-1" y="-1" width="7" height="7" fill="#120d1f"/>';

    celdas.forEach(({ x, y, lima }) => {
        const color = lima ? "#d6ff5e" : "#a07cff";
        html += '<rect x="' + x + '" y="' + y + '" width="1" height="1" fill="' + color + '"/>';

        if (x !== 2) {
            html += '<rect x="' + (4 - x) + '" y="' + y + '" width="1" height="1" fill="' + color + '"/>';
        }
    });

    svg.innerHTML = html;

    return svg;
}

/* notificaciones */

function notificar(tipo, titulo, mensaje) {
    let contenedor = document.getElementById("notificaciones");

    if (!contenedor) {
        contenedor = crearElemento("div", "notificaciones");
        contenedor.id = "notificaciones";
        document.body.append(contenedor);
    }

    const notificacion = crearElemento("div", "notificacion " + tipo);
    notificacion.setAttribute("role", tipo === "error" ? "alert" : "status");

    // barra de título tipo ventana de juego
    const barra = crearElemento("div", "notificacion-barra");
    barra.append(crearElemento("span", "", tipo === "error" ? "ERROR.EXE" : "LOBBY.EXE"));

    const cerrar = crearElemento("button", "notificacion-cerrar", "X");
    cerrar.type = "button";
    cerrar.setAttribute("aria-label", "Cerrar notificación");
    barra.append(cerrar);

    const cuerpo = crearElemento("div", "notificacion-cuerpo");

    const contenido = crearElemento("div", "notificacion-contenido");
    contenido.append(
        crearElemento("strong", "notificacion-titulo", titulo),
        crearElemento("p", "notificacion-mensaje", mensaje)
    );

    cuerpo.append(crearBicho(tipo === "error" ? "triste" : "feliz"), contenido);

    const progreso = crearElemento("div", "notificacion-progreso");

    notificacion.append(barra, cuerpo, progreso);
    contenedor.append(notificacion);

    let cerrada = false;

    function quitar() {
        if (cerrada) {
            return;
        }

        cerrada = true;
        notificacion.classList.add("saliendo");
        setTimeout(() => notificacion.remove(), 250);
    }

    cerrar.addEventListener("click", quitar);
    setTimeout(quitar, 5000);
}

/* ==========================================================
   modo demo (ServidorDemo)
   Solo ServidorDemo responde "OK" en /health. En ese servidor
   el acceso y las postulaciones están deshabilitados.
   ========================================================== */

async function detectarDemo() {
    try {
        const respuesta = await fetch("/health", { cache: "no-store" });
        const texto = await respuesta.text();

        modoDemo = respuesta.ok && texto.trim() === "OK";
    } catch (error) {
        modoDemo = false;
    }
}

function aplicarModoDemo() {
    if (!modoDemo) {
        return;
    }

    document.body.classList.add("modo-demo");

    const aviso = crearElemento("div", "demo-aviso");
    aviso.append(
        crearElemento("strong", "", "Demo pública"),
        crearElemento(
            "p",
            "",
            "En esta versión podés explorar las partidas. El registro, el login " +
            "y las postulaciones funcionan en la versión completa."
        )
    );

    contenidoCuenta.before(aviso);

    [formLogin, formRegistro].forEach(formulario => {
        formulario.querySelectorAll("input, select, button").forEach(campo => {
            campo.disabled = true;
        });
    });
}

/* ==========================================================
   partidas
   ========================================================== */

function crearBotonPostular(scrim) {
    const boton = crearElemento("button", "postular-button");
    boton.type = "button";

    if (modoDemo) {
        boton.textContent = "NO DISPONIBLE EN DEMO";
        boton.disabled = true;
        return boton;
    }

    const postulacion = postulaciones.find(p => p.scrimId === scrim.id);
    const disponible =
        scrim.estado === "Buscando jugadores" &&
        scrim.cuposDisponibles > 0;

    if (postulacion) {
        boton.textContent = postulacion.estado === "Pendiente"
            ? "POSTULACIÓN ENVIADA"
            : "SOLICITUD " + String(postulacion.estado).toUpperCase();
        boton.classList.add("enviado");
        boton.disabled = true;
        return boton;
    }

    if (!disponible) {
        boton.textContent = "NO DISPONIBLE";
        boton.disabled = true;
        return boton;
    }

    // solo los jugadores pueden postularse (igual que en tu versión)
    if (usuario && usuario.rol !== "JUGADOR") {
        boton.textContent = "SOLO JUGADORES";
        boton.disabled = true;
        return boton;
    }

    if (!usuario) {
        boton.textContent = "INGRESÁ PARA POSTULARTE";

        boton.addEventListener("click", () => {
            notificar(
                "error",
                "Necesitás una cuenta",
                "Iniciá sesión o registrate para postularte a esta partida."
            );
            irA("mi-cuenta");
            setTimeout(() => document.getElementById("loginEmail")?.focus({ preventScroll: true }), 500);
        });

        return boton;
    }

    boton.textContent = "POSTULARME";
    boton.addEventListener("click", () => postular(scrim, boton));

    return boton;
}

function crearTarjeta(scrim) {
    const tarjeta = crearElemento("article", "scrim-card");

    const superior = crearElemento("div", "card-top");

    const icono = crearElemento("div", "game-icon", iconosJuegos[scrim.juego] || "ES");

    const buscando = scrim.estado === "Buscando jugadores";

    const estado = crearElemento(
        "span",
        buscando ? "status" : "status other",
        buscando ? "BUSCANDO" : String(scrim.estado).toUpperCase()
    );

    superior.append(icono, estado);

    const titulo = crearElemento("h4", "", nombreJuego(scrim.juego));

    const subtitulo = crearElemento(
        "p",
        "card-subtitle",
        "Partida competitiva · " + scrim.formato
    );

    const detalles = crearElemento("div", "card-details");

    detalles.append(
        crearDetalle("Región", scrim.region),
        crearDetalle("Formato", scrim.formato),
        crearDetalle("Fecha", formatearFecha(scrim.fecha))
    );

    const inferior = crearElemento("div", "card-bottom");

    const cupos = crearElemento("div", "cupos");

    const cuposTexto = crearElemento(
        "span",
        "cupos-texto",
        scrim.cuposDisponibles === 1
            ? "1 cupo disponible"
            : scrim.cuposDisponibles + " cupos disponibles"
    );

    // si el servidor manda cuposTotales, se muestran como slots
    if (Number.isInteger(scrim.cuposTotales) && scrim.cuposTotales > 0) {
        const slots = crearElemento("div", "cupos-slots");
        slots.setAttribute("aria-hidden", "true");

        const ocupados = Math.max(0, scrim.cuposTotales - scrim.cuposDisponibles);

        for (let i = 0; i < scrim.cuposTotales; i++) {
            slots.append(crearElemento("i", i < ocupados ? "ocupado" : ""));
        }

        cuposTexto.textContent =
            scrim.cuposDisponibles + " de " + scrim.cuposTotales + " cupos libres";

        cupos.append(slots);
    }

    cupos.append(cuposTexto, crearBotonPostular(scrim));

    const identificador = crearElemento("span", "card-id", "SCRIM #" + scrim.id);

    inferior.append(cupos, identificador);

    tarjeta.append(superior, titulo, subtitulo, detalles, inferior);

    return tarjeta;
}

// bicho: "feliz", "triste", "cargando" o null para mostrar solo texto
function mostrarMensaje(contenedor, mensaje, bicho = "triste", detalle) {
    const tarjeta = crearElemento("div", "message-card");

    if (bicho) {
        tarjeta.append(crearBicho(bicho));
    }

    tarjeta.append(crearElemento("p", "", mensaje));

    if (detalle) {
        tarjeta.append(crearElemento("small", "message-detalle", detalle));
    }

    contenedor.replaceChildren(tarjeta);
}

function actualizarFiltros() {
    const juegoElegido = filtroJuego.value;
    const regionElegida = filtroRegion.value;

    const juegos = [...new Set(scrims.map(scrim => scrim.juego))].sort();
    const regiones = [...new Set(scrims.map(scrim => scrim.region))].sort();

    filtroJuego.replaceChildren(new Option("Todos los juegos", ""));
    filtroRegion.replaceChildren(new Option("Todas las regiones", ""));

    juegos.forEach(juego => {
        filtroJuego.add(new Option(nombreJuego(juego), juego));
    });

    regiones.forEach(region => {
        filtroRegion.add(new Option(region, region));
    });

    // conservar lo que el usuario tenía elegido al recargar
    if (juegos.includes(juegoElegido)) {
        filtroJuego.value = juegoElegido;
    }

    if (regiones.includes(regionElegida)) {
        filtroRegion.value = regionElegida;
    }
}

function mostrarScrims() {
    if (!scrimsCargadas) {
        return;
    }

    const texto = busqueda.value.trim().toLowerCase();
    const juego = filtroJuego.value;
    const region = filtroRegion.value;

    const filtrados = scrims.filter(scrim => {
        const coincideTexto =
            nombreJuego(scrim.juego).toLowerCase().includes(texto) ||
            scrim.juego.toLowerCase().includes(texto);

        const coincideJuego = !juego || scrim.juego === juego;
        const coincideRegion = !region || scrim.region === region;

        return coincideTexto && coincideJuego && coincideRegion;
    });

    contador.textContent =
        filtrados.length === 1 ? "1 partida" : filtrados.length + " partidas";

    estadoCarga.textContent = "Datos del servidor eScrims";

    if (filtrados.length === 0) {
        mostrarMensaje(
            listaScrims,
            "No encontramos partidas con esos filtros.",
            "triste",
            "PROBÁ OTRO JUEGO O REGIÓN"
        );
        return;
    }

    listaScrims.replaceChildren(...filtrados.map(crearTarjeta));
}

async function cargarScrims() {
    estadoCarga.textContent = "Cargando partidas...";

    // la primera vez se muestra la mascota saltando
    if (!scrimsCargadas) {
        mostrarMensaje(listaScrims, "Cargando partidas", "cargando");
        listaScrims.querySelector(".message-card p").classList.add("puntos");
    }

    try {
        const datos = await api("/api/scrims");

        if (!Array.isArray(datos)) {
            throw new Error("Respuesta inesperada del servidor");
        }

        scrims = datos;
        scrimsCargadas = true;

        actualizarFiltros();
        mostrarScrims();

    } catch (error) {
        console.error("No se pudieron cargar las partidas:", error);

        contador.textContent = "Sin conexión";
        estadoCarga.textContent = "Error de conexión";

        mostrarMensaje(
            listaScrims,
            "No se pudieron cargar las partidas.",
            "triste",
            "COMPROBÁ QUE EL SERVIDOR JAVA ESTÉ CORRIENDO"
        );
    }
}

async function postular(scrim, boton) {
    boton.disabled = true;
    boton.textContent = "ENVIANDO...";

    try {
        await enviarFormulario("/api/postulaciones", { scrimId: scrim.id });

        notificar(
            "exito",
            "Postulación enviada",
            "Te postulaste a " + nombreJuego(scrim.juego) + " · " +
            scrim.formato + " · " + scrim.region + ". El organizador la va a revisar."
        );

        await cargarPostulaciones();
        await cargarOrganizador();
        await cargarScrims();

    } catch (error) {
        if (error.status === 401) {
            sesionVencida();
            return;
        }

        notificar("error", "No se pudo postular", error.message);

        boton.disabled = false;
        boton.textContent = "POSTULARME";
    }
}

/* ==========================================================
   cuenta
   ========================================================== */

function actualizarVistaCuenta() {
    const conSesion = usuario !== null;
    const esOrganizador = conSesion && usuario.rol === "ORGANIZADOR";

    contenidoCuenta.hidden = conSesion;
    perfilCuenta.hidden = !conSesion;

    seccionPostulaciones.hidden = !conSesion;
    navPostulaciones.hidden = !conSesion;

    seccionOrganizador.hidden = !esOrganizador;
    navOrganizador.hidden = !esOrganizador;

    if (conSesion) {
        cuentaDescripcion.textContent =
            "Sesión iniciada. Ya podés postularte desde la lista de partidas.";

        document.getElementById("perfilInicial").replaceChildren(
            crearAvatar(usuario.username)
        );
        document.getElementById("perfilNombre").textContent = usuario.username;
        document.getElementById("perfilRegion").textContent = usuario.region || "—";
        document.getElementById("perfilRol").textContent =
            usuario.rol === "ORGANIZADOR" ? "Organizador" : "Jugador";
        document.getElementById("perfilId").textContent = "#" + usuario.id;
    } else {
        cuentaDescripcion.textContent = modoDemo
            ? "El acceso está deshabilitado en la demo pública."
            : "Iniciá sesión o creá una cuenta para postularte a partidas.";
        postulaciones = [];
        listaPostulaciones.replaceChildren();
        listaOrganizador.replaceChildren();
    }

    mostrarScrims();
    actualizarIndicador();
}

async function cargarSesion() {
    try {
        const datos = await api("/api/auth/me");
        usuario = datos && datos.autenticado ? datos : null;
    } catch (error) {
        usuario = null;
    }

    actualizarVistaCuenta();

    if (usuario) {
        await cargarDatosDeUsuario();
    }
}

async function cargarDatosDeUsuario() {
    await cargarPostulaciones();

    if (usuario && usuario.rol === "ORGANIZADOR") {
        await cargarOrganizador();
    }
}

function sesionVencida() {
    usuario = null;
    actualizarVistaCuenta();

    notificar(
        "error",
        "Sesión vencida",
        "Volvé a iniciar sesión para continuar."
    );
}

async function enviarAcceso(evento, url, esRegistro) {
    evento.preventDefault();

    if (modoDemo) {
        return;
    }

    const formulario = evento.currentTarget;
    const boton = formulario.querySelector("button[type='submit']");
    const datos = Object.fromEntries(new FormData(formulario));

    const faltantes = [...formulario.querySelectorAll("[required]")]
        .filter(campo => !String(campo.value).trim());

    if (faltantes.length > 0) {
        notificar("error", "Faltan datos", "Completá todos los campos.");
        faltantes[0].focus();
        return;
    }

    const contrasena = formulario.querySelector("[name='password']");

    if (contrasena && contrasena.value.length < 8) {
        notificar(
            "error",
            "Contraseña muy corta",
            "La contraseña tiene que tener al menos 8 caracteres."
        );
        contrasena.focus();
        return;
    }

    const textoBoton = boton.textContent;
    boton.disabled = true;
    boton.textContent = "ENVIANDO...";

    try {
        usuario = await enviarFormulario(url, datos);

        notificar(
            "exito",
            esRegistro ? "Cuenta creada" : "Sesión iniciada",
            "Hola, " + usuario.username + ". Ya podés postularte a partidas."
        );

        formLogin.reset();
        formRegistro.reset();

        actualizarVistaCuenta();
        await cargarDatosDeUsuario();

    } catch (error) {
        notificar(
            "error",
            esRegistro ? "No se pudo crear la cuenta" : "No se pudo iniciar sesión",
            error.message
        );

    } finally {
        boton.disabled = false;
        boton.textContent = textoBoton;
    }
}

async function cerrarSesion() {
    cerrarSesionBoton.disabled = true;

    try {
        await enviarFormulario("/api/auth/logout", {});
    } catch (error) {
        console.error("Error al cerrar sesión:", error);
    }

    cerrarSesionBoton.disabled = false;
    usuario = null;
    actualizarVistaCuenta();

    notificar("exito", "Sesión cerrada", "Hasta la próxima partida.");
}

/* ==========================================================
   mis postulaciones
   ========================================================== */

function crearPostulacion(postulacion) {
    const tarjeta = crearElemento("article", "postulacion-card");
    const clase = claseEstado(postulacion.estado);

    const cabecera = crearElemento("div", "postulacion-cabecera");

    const titulo = crearElemento("div");
    titulo.append(
        crearElemento("span", "postulacion-id", "POSTULACIÓN #" + postulacion.id),
        crearElemento("h4", "postulacion-juego", nombreJuego(postulacion.juego))
    );

    const estado = crearElemento(
        "span",
        "postulacion-estado " + clase,
        String(postulacion.estado).toUpperCase()
    );

    cabecera.append(titulo, estado);

    const detalles = crearElemento("div", "postulacion-detalles");
    detalles.append(
        crearDetalle("Scrim", "#" + postulacion.scrimId),
        crearDetalle("Región", postulacion.region),
        crearDetalle("Formato", postulacion.formato),
        crearDetalle("Fecha", formatearFecha(postulacion.fecha))
    );

    const mensajes = {
        pendiente: "El organizador todavía no revisó tu solicitud.",
        aceptada: "¡Estás adentro! Te esperamos en el lobby.",
        rechazada: "Esta vez no quedaste. Probá con otra partida."
    };

    tarjeta.append(
        cabecera,
        detalles,
        crearElemento("p", "postulacion-mensaje", mensajes[clase])
    );

    return tarjeta;
}

async function cargarPostulaciones() {
    if (!usuario) {
        return;
    }

    try {
        const datos = await api("/api/postulaciones");
        postulaciones = Array.isArray(datos) ? datos : [];

        if (postulaciones.length === 0) {
            mostrarMensaje(
                listaPostulaciones,
                "Todavía no te postulaste a ninguna partida."
            );
        } else {
            listaPostulaciones.replaceChildren(
                ...postulaciones.map(crearPostulacion)
            );
        }

        mostrarScrims();

    } catch (error) {
        if (error.status === 401) {
            sesionVencida();
            return;
        }

        mostrarMensaje(listaPostulaciones, "No se pudieron cargar tus postulaciones.");
    }
}

/* ==========================================================
   panel organizador
   ========================================================== */

function crearSolicitud(solicitud) {
    const tarjeta = crearElemento("article", "organizador-card");
    const clase = claseEstado(solicitud.estado);

    const cabecera = crearElemento("div", "organizador-card-cabecera");

    const titulo = crearElemento("div");
    titulo.append(
        crearElemento("span", "postulacion-id", "SOLICITUD #" + solicitud.id + " · SCRIM #" + solicitud.scrimId),
        crearElemento("h4", "organizador-juego", nombreJuego(solicitud.juego)),
        (() => {
            const jugador = crearElemento("p", "organizador-jugador");
            jugador.append(
                crearAvatar(solicitud.usuario),
                document.createTextNode("Jugador: " + solicitud.usuario)
            );
            return jugador;
        })()
    );

    cabecera.append(
        titulo,
        crearElemento(
            "span",
            "postulacion-estado " + clase,
            String(solicitud.estado).toUpperCase()
        )
    );

    const detalles = crearElemento("div", "organizador-card-detalles");
    detalles.append(
        crearDetalle("Región", solicitud.region),
        crearDetalle("Formato", solicitud.formato),
        crearDetalle("Fecha", formatearFecha(solicitud.fecha))
    );

    tarjeta.append(cabecera, detalles);

    if (clase === "pendiente") {
        const acciones = crearElemento("div", "organizador-card-acciones");

        const aceptar = crearElemento("button", "organizador-aceptar", "ACEPTAR");
        aceptar.type = "button";

        const rechazar = crearElemento("button", "organizador-rechazar", "RECHAZAR");
        rechazar.type = "button";

        aceptar.addEventListener("click", () =>
            gestionarSolicitud(solicitud, "aceptar", [aceptar, rechazar])
        );

        rechazar.addEventListener("click", () =>
            gestionarSolicitud(solicitud, "rechazar", [aceptar, rechazar])
        );

        acciones.append(aceptar, rechazar);
        tarjeta.append(acciones);

    } else {
        tarjeta.append(
            crearElemento(
                "p",
                "organizador-resuelto",
                "Solicitud resuelta: " + String(solicitud.estado).toLowerCase() + "."
            )
        );
    }

    return tarjeta;
}

async function cargarOrganizador() {
    if (!usuario || usuario.rol !== "ORGANIZADOR") {
        return;
    }

    try {
        const datos = await api("/api/organizador/postulaciones");
        const solicitudes = Array.isArray(datos) ? datos : [];

        const contar = clase =>
            solicitudes.filter(s => claseEstado(s.estado) === clase).length;

        document.getElementById("resumenPendientes").textContent = contar("pendiente");
        document.getElementById("resumenAceptadas").textContent = contar("aceptada");
        document.getElementById("resumenRechazadas").textContent = contar("rechazada");

        if (solicitudes.length === 0) {
            mostrarMensaje(listaOrganizador, "Todavía no llegaron solicitudes.", "feliz");
            return;
        }

        // pendientes primero
        solicitudes.sort((a, b) =>
            (claseEstado(a.estado) === "pendiente" ? 0 : 1) -
            (claseEstado(b.estado) === "pendiente" ? 0 : 1)
        );

        listaOrganizador.replaceChildren(...solicitudes.map(crearSolicitud));

    } catch (error) {
        if (error.status === 401) {
            sesionVencida();
            return;
        }

        mostrarMensaje(listaOrganizador, "No se pudieron cargar las solicitudes.");
    }
}

async function gestionarSolicitud(solicitud, accion, botones) {
    botones.forEach(boton => {
        boton.disabled = true;
    });

    try {
        await enviarFormulario("/api/organizador/postulaciones", {
            postulacionId: solicitud.id,
            accion
        });

        notificar(
            "exito",
            accion === "aceptar" ? "Jugador aceptado" : "Solicitud rechazada",
            solicitud.usuario + " · " + nombreJuego(solicitud.juego)
        );

        await cargarOrganizador();
        await cargarPostulaciones();
        await cargarScrims();

    } catch (error) {
        if (error.status === 401) {
            sesionVencida();
            return;
        }

        notificar("error", "No se pudo procesar", error.message);

        botones.forEach(boton => {
            boton.disabled = false;
        });
    }
}

/* ==========================================================
   indicador de sección
   ========================================================== */

const header = document.querySelector(".header");
const indicador = document.querySelector(".section-indicator");
const enlacesNav = [...document.querySelectorAll(".nav-link")];

let seccionActual = null;
let temporizadorIndicador;
let indicadorPendiente = false;

function seccionesVisibles() {
    return [...document.querySelectorAll("[data-seccion]")]
        .filter(seccion => !seccion.hidden && seccion.offsetParent !== null);
}

function marcarEnlace(id) {
    enlacesNav.forEach(enlace => {
        const activo = enlace.getAttribute("href") === "#" + id;

        enlace.classList.toggle("active", activo);

        if (activo) {
            enlace.setAttribute("aria-current", "location");

            // en celular el menú se desliza: llevar el link activo a la vista
            const menu = enlace.parentElement;

            if (menu.scrollWidth > menu.clientWidth) {
                menu.scrollTo({ left: enlace.offsetLeft - 16, behavior: "smooth" });
            }
        } else {
            enlace.removeAttribute("aria-current");
        }
    });
}

function escribirIndicador(seccion) {
    indicador.querySelector(".indicator-round").textContent = seccion.dataset.ronda || "";
    indicador.querySelector(".indicator-label").textContent = seccion.dataset.seccion;
}

function mostrarSeccion(seccion) {
    if (seccion === seccionActual) {
        return;
    }

    const primeraVez = seccionActual === null;
    seccionActual = seccion;
    marcarEnlace(seccion.id);

    if (primeraVez) {
        escribirIndicador(seccion);
        return;
    }

    clearTimeout(temporizadorIndicador);
    indicador.classList.add("saliendo");

    temporizadorIndicador = setTimeout(() => {
        escribirIndicador(seccion);
        indicador.classList.remove("saliendo");
        indicador.classList.add("entrando");
        void indicador.offsetWidth;
        indicador.classList.remove("entrando");
    }, 180);
}

function actualizarIndicador() {
    if (!header || !indicador) {
        return;
    }

    const secciones = seccionesVisibles();

    if (secciones.length === 0) {
        return;
    }

    // la sección activa es la última que pasó el 40% de la pantalla
    const linea = window.innerHeight * 0.4;
    let elegida = secciones[0];

    secciones.forEach(seccion => {
        if (seccion.getBoundingClientRect().top <= linea) {
            elegida = seccion;
        }
    });

    const alto = document.documentElement.scrollHeight;

    if (window.innerHeight + window.scrollY >= alto - 2) {
        elegida = secciones[secciones.length - 1];
    }

    mostrarSeccion(elegida);

    const recorrido = alto - window.innerHeight;
    const progreso = recorrido > 0 ? window.scrollY / recorrido : 0;
    header.style.setProperty("--progreso", progreso.toFixed(4));
}

window.addEventListener("scroll", () => {
    if (indicadorPendiente) {
        return;
    }

    indicadorPendiente = true;

    requestAnimationFrame(() => {
        indicadorPendiente = false;
        actualizarIndicador();
    });
}, { passive: true });

window.addEventListener("resize", actualizarIndicador);

/* ==========================================================
   terminal de "Cómo funciona": se anima al entrar en pantalla
   ========================================================== */

function animarTerminal() {
    const terminal = document.querySelector(".workflow-terminal");

    if (!terminal) {
        return;
    }

    const contadorTerminal = terminal.querySelector(".workflow-progress-info strong");
    const objetivo = 7;

    function arrancar() {
        terminal.classList.add("visible");

        if (!contadorTerminal || matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return;
        }

        let actual = 0;
        contadorTerminal.textContent = "0 / 10";

        const intervalo = setInterval(() => {
            actual++;
            contadorTerminal.textContent = actual + " / 10";

            if (actual >= objetivo) {
                clearInterval(intervalo);
            }
        }, 1400 / objetivo);
    }

    if (!("IntersectionObserver" in window)) {
        arrancar();
        return;
    }

    const observador = new IntersectionObserver(entradas => {
        if (entradas.some(entrada => entrada.isIntersecting)) {
            observador.disconnect();
            setTimeout(arrancar, 200);
        }
    }, { threshold: 0.4 });

    observador.observe(terminal);
}

/* ==========================================================
   easter egg: ↑ ↑ ↓ ↓ ← → ← → B A
   ========================================================== */

function activarKonami() {
    const secuencia = [
        "ArrowUp", "ArrowUp", "ArrowDown", "ArrowDown",
        "ArrowLeft", "ArrowRight", "ArrowLeft", "ArrowRight",
        "b", "a"
    ];

    let posicion = 0;
    let enCurso = false;

    document.addEventListener("keydown", evento => {
        // no interferir mientras se escribe en un formulario
        if (evento.target.closest("input, select, textarea")) {
            posicion = 0;
            return;
        }

        const tecla = evento.key.length === 1 ? evento.key.toLowerCase() : evento.key;

        if (tecla === secuencia[posicion]) {
            posicion++;
        } else {
            posicion = tecla === secuencia[0] ? 1 : 0;
        }

        if (posicion === secuencia.length) {
            posicion = 0;

            if (!enCurso) {
                enCurso = true;
                lanzarKonami(() => {
                    enCurso = false;
                });
            }
        }
    });
}

function lanzarKonami(alTerminar) {
    const escena = crearElemento("div", "konami");
    escena.setAttribute("aria-hidden", "true");

    const corredor = crearBicho("cargando");
    corredor.classList.add("konami-bicho");

    escena.append(
        crearElemento("span", "konami-gg", "GG!"),
        corredor
    );

    document.body.append(escena);

    setTimeout(() => {
        escena.remove();
        alTerminar();
    }, 3200);
}

/* ==========================================================
   eventos e inicio
   ========================================================== */

busqueda.addEventListener("input", mostrarScrims);
filtroJuego.addEventListener("change", mostrarScrims);
filtroRegion.addEventListener("change", mostrarScrims);

limpiarFiltros.addEventListener("click", () => {
    busqueda.value = "";
    filtroJuego.value = "";
    filtroRegion.value = "";

    mostrarScrims();
});

formLogin.addEventListener("submit", evento =>
    enviarAcceso(evento, "/api/auth/login", false)
);

formRegistro.addEventListener("submit", evento =>
    enviarAcceso(evento, "/api/auth/register", true)
);

cerrarSesionBoton.addEventListener("click", cerrarSesion);
actualizarPostulacionesBoton.addEventListener("click", cargarPostulaciones);
actualizarOrganizadorBoton.addEventListener("click", cargarOrganizador);

actualizarIndicador();
animarTerminal();
activarKonami();

(async function iniciar() {
    await detectarDemo();
    aplicarModoDemo();
    await cargarSesion();
    await cargarScrims();
    actualizarIndicador();
})();