
const listaScrims = document.getElementById("listaScrims");
const contador = document.getElementById("contador");
const estadoCarga = document.getElementById("estadoCarga");

const busqueda = document.getElementById("busqueda");
const filtroJuego = document.getElementById("filtroJuego");
const filtroRegion = document.getElementById("filtroRegion");
const limpiarFiltros = document.getElementById("limpiarFiltros");

let scrims = [];

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
    const etiqueta = crearElemento("span", "", nombre);
    const contenido = crearElemento("strong", "", String(valor));

    fila.append(etiqueta, contenido);

    return fila;
}

function crearTarjeta(scrim) {
    const tarjeta = crearElemento("article", "scrim-card");

    const superior = crearElemento("div", "card-top");

    const icono = crearElemento(
        "div",
        "game-icon",
        iconosJuegos[scrim.juego] || "ES"
    );

    const buscando = scrim.estado === "Buscando jugadores";

    const estado = crearElemento(
        "span",
        buscando ? "status" : "status other",
        buscando ? "BUSCANDO" : scrim.estado.toUpperCase()
    );

    superior.append(icono, estado);

    const titulo = crearElemento(
        "h4",
        "",
        nombresJuegos[scrim.juego] || scrim.juego
    );

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

    const cupos = crearElemento(
        "span",
        "cupos",
        scrim.cuposDisponibles + " cupos disponibles"
    );

    const identificador = crearElemento(
        "span",
        "card-id",
        "SCRIM #" + scrim.id
    );

    inferior.append(cupos, identificador);

    tarjeta.append(
        superior,
        titulo,
        subtitulo,
        detalles,
        inferior
    );

    return tarjeta;
}

function mostrarMensaje(mensaje) {
    listaScrims.replaceChildren(
        crearElemento("div", "message-card", mensaje)
    );
}

function actualizarFiltros() {
    const juegos = [...new Set(scrims.map(scrim => scrim.juego))].sort();
    const regiones = [...new Set(scrims.map(scrim => scrim.region))].sort();

    filtroJuego.replaceChildren(
        new Option("Todos los juegos", "")
    );

    filtroRegion.replaceChildren(
        new Option("Todas las regiones", "")
    );

    juegos.forEach(juego => {
        filtroJuego.add(
            new Option(nombresJuegos[juego] || juego, juego)
        );
    });

    regiones.forEach(region => {
        filtroRegion.add(new Option(region, region));
    });
}

function mostrarScrims() {
    const texto = busqueda.value.trim().toLowerCase();
    const juego = filtroJuego.value;
    const region = filtroRegion.value;

    const filtrados = scrims.filter(scrim => {
        const nombreJuego = nombresJuegos[scrim.juego] || scrim.juego;

        const coincideTexto =
            nombreJuego.toLowerCase().includes(texto) ||
            scrim.juego.toLowerCase().includes(texto);

        const coincideJuego = !juego || scrim.juego === juego;
        const coincideRegion = !region || scrim.region === region;

        return coincideTexto && coincideJuego && coincideRegion;
    });

    contador.textContent = filtrados.length + " partidas";
    estadoCarga.textContent = "Datos del servidor eScrims";

    if (filtrados.length === 0) {
        mostrarMensaje("No encontramos partidas con esos filtros.");
        return;
    }

    listaScrims.replaceChildren(
        ...filtrados.map(crearTarjeta)
    );
}

async function cargarScrims() {
    estadoCarga.textContent = "Cargando partidas...";

    try {
        const respuesta = await fetch("/api/scrims", {
            method: "GET",
            cache: "no-store"
        });

        if (!respuesta.ok) {
            throw new Error("Error HTTP " + respuesta.status);
        }

        const datos = await respuesta.json();

        if (!Array.isArray(datos)) {
            throw new Error("Respuesta inesperada del servidor");
        }

        scrims = datos;

        actualizarFiltros();
        mostrarScrims();

    } catch (error) {
        console.error("No se pudieron cargar las partidas:", error);

        contador.textContent = "Sin conexión";
        estadoCarga.textContent = "Error de conexión";

        mostrarMensaje(
            "No se pudieron cargar las partidas. " +
            "Comprobá que el servidor Java esté ejecutándose."
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

cargarScrims();
