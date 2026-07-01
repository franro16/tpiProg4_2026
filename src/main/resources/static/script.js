var API_BASE = window.location.port === "5500" ? "http://localhost:8080" : "";

var session = cargarSesion();
var subastasCache = [];
var productosCache = [];
var categoriasCache = [];

function $(selector) {
  return document.querySelector(selector);
}

function $$(selector) {
  return Array.prototype.slice.call(document.querySelectorAll(selector));
}

function cargarSesion() {
  try {
    return JSON.parse(localStorage.getItem("subastas_session"));
  } catch (error) {
    return null;
  }
}

function guardarSesion(datosSesion) {
  session = datosSesion;
  localStorage.setItem("subastas_session", JSON.stringify(datosSesion));
  renderizarSesion();
}

function cerrarSesion() {
  session = null;
  localStorage.removeItem("subastas_session");

  subastasCache = [];
  productosCache = [];
  categoriasCache = [];

  limpiarPanelesPrivados();

  renderizarSesion();
  cargarDatosPublicos();
}

function limpiarPanelesPrivados() {
  var detalle = $("#auctionDetail");
  if (detalle) {
    detalle.classList.add("hidden");
    detalle.innerHTML = "";
  }

  var notificaciones = $("#notificationList");
  if (notificaciones) {
    notificaciones.innerHTML = "";
  }

  var reclamos = $("#myDisputeList");
  if (reclamos) {
    reclamos.innerHTML = "";
  }

  var adminUsuarios = $("#adminUserList");
  if (adminUsuarios) {
    adminUsuarios.innerHTML = "";
  }

  var adminReclamos = $("#adminDisputeList");
  if (adminReclamos) {
    adminReclamos.innerHTML = "";
  }
}

function normalizarRol(rol) {
  return String(rol || "")
    .replace("ROLE_", "")
    .trim()
    .toUpperCase();
}

function obtenerRoles() {
  if (!session || !session.role) {
    return [];
  }

  return String(session.role)
    .split(",")
    .map(normalizarRol)
    .filter(function (rol) {
      return rol !== "";
    });
}

function tieneRol(rolBuscado) {
  return obtenerRoles().indexOf(normalizarRol(rolBuscado)) !== -1;
}

function estaLogueado() {
  return Boolean(session && session.token);
}

function mostrarMensaje(texto, tipo) {
  var area = $("#messageArea");

  if (!area) {
    alert(texto);
    return;
  }

  var div = document.createElement("div");
  div.className = "alert " + (tipo || "success");
  div.textContent = texto;
  area.appendChild(div);

  setTimeout(function () {
    div.remove();
  }, 5000);
}

function obtenerMensajeError(error) {
  if (!error) {
    return "Ocurrió un error";
  }

  if (error.fieldErrors) {
    return Object.keys(error.fieldErrors)
      .map(function (campo) {
        return campo + ": " + error.fieldErrors[campo];
      })
      .join(" | ");
  }

  if (error.message) {
    return error.message;
  }

  if (error.error) {
    return error.error;
  }

  if (typeof error === "string") {
    return error;
  }

  return "Ocurrió un error";
}

function api(ruta, opciones) {
  opciones = opciones || {};

  var headers = opciones.headers || {};

  if (opciones.body) {
    headers["Content-Type"] = "application/json";
  }

  if (session && session.token) {
    headers["Authorization"] = "Bearer " + session.token;
  }

  return fetch(API_BASE + ruta, {
    method: opciones.method || "GET",
    headers: headers,
    body: opciones.body
  }).then(function (respuesta) {
    return respuesta.text().then(function (texto) {
      var datos = null;

      if (texto) {
        try {
          datos = JSON.parse(texto);
        } catch (error) {
          datos = texto;
        }
      }

      if (!respuesta.ok) {
        throw datos || { message: "Error HTTP " + respuesta.status };
      }

      return datos;
    });
  });
}

function escaparHTML(valor) {
  return String(valor || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatearPrecio(valor) {
  var numero = Number(valor || 0);

  return numero.toLocaleString("es-AR", {
    style: "currency",
    currency: "ARS"
  });
}

function formatearFecha(valor) {
  if (!valor) {
    return "-";
  }

  var fecha = new Date(valor);

  if (isNaN(fecha.getTime())) {
    return valor;
  }

  return fecha.toLocaleString("es-AR");
}

function fechaLocalAIso(valor) {
  if (!valor) return null;

  return valor.toString().substring(0, 16);
}

function formatearFechaParaInput(fecha) {
  var anio = fecha.getFullYear();
  var mes = String(fecha.getMonth() + 1).padStart(2, "0");
  var dia = String(fecha.getDate()).padStart(2, "0");
  var hora = String(fecha.getHours()).padStart(2, "0");
  var minutos = String(fecha.getMinutes()).padStart(2, "0");

  return anio + "-" + mes + "-" + dia + "T" + hora + ":" + minutos;
}

function obtenerFechaFuturaParaInput(minutosExtra) {
  var fecha = new Date();
  fecha.setMinutes(fecha.getMinutes() + minutosExtra);
  fecha.setSeconds(0);
  fecha.setMilliseconds(0);

  return formatearFechaParaInput(fecha);
}

function configurarFechasSubasta() {
  var auctionForm = $("#auctionForm");

  if (!auctionForm) {
    return;
  }

  var inputInicio = auctionForm.querySelector("[name='startDate']");
  var inputCierre = auctionForm.querySelector("[name='endDate']");

  if (!inputInicio || !inputCierre) {
    return;
  }

  var inicioMinimo = obtenerFechaFuturaParaInput(2);
  var cierreMinimo = obtenerFechaFuturaParaInput(3);

  inputInicio.min = inicioMinimo;
  inputCierre.min = cierreMinimo;

  if (!inputInicio.value) {
    inputInicio.value = inicioMinimo;
  }

  if (!inputCierre.value) {
    inputCierre.value = obtenerFechaFuturaParaInput(10);
  }

  inputInicio.addEventListener("change", function () {
    if (!inputInicio.value) {
      return;
    }

    var inicio = new Date(inputInicio.value);
    var cierreActual = new Date(inputCierre.value);

    if (isNaN(inicio.getTime())) {
      return;
    }

    var cierreMinimoSegunInicio = new Date(inicio.getTime() + 60000);
    var cierreMinimoFormateado = formatearFechaParaInput(cierreMinimoSegunInicio);

    inputCierre.min = cierreMinimoFormateado;

    if (!inputCierre.value || isNaN(cierreActual.getTime()) || cierreActual <= inicio) {
      inputCierre.value = cierreMinimoFormateado;
    }
  });
}

function cambiarSeccion(idSeccion) {
  $$(".section").forEach(function (seccion) {
    seccion.classList.remove("active");
  });

  var seccionActiva = $("#" + idSeccion);

  if (seccionActiva) {
    seccionActiva.classList.add("active");
  }

  $$(".nav-btn").forEach(function (boton) {
    boton.classList.remove("active");

    if (boton.dataset.section === idSeccion) {
      boton.classList.add("active");
    }
  });

  if (idSeccion === "sectionNotifications" && estaLogueado()) {
    cargarNotificaciones();
  }

  if (idSeccion === "sectionDisputes" && estaLogueado()) {
    cargarMisReclamos();
  }

  if (idSeccion === "sectionAdmin" && tieneRol("ADMIN")) {
    cargarDatosAdmin();
  }
}

function renderizarSesion() {
  var info = $("#sessionInfo");
  var botonLogout = $("#logoutBtn");

  if (!info || !botonLogout) {
    return;
  }

  if (!session) {
    info.textContent = "Sin sesión iniciada";
    botonLogout.classList.add("hidden");
  } else {
    info.textContent = session.username + " · " + obtenerRoles().join(", ");
    botonLogout.classList.remove("hidden");
  }

  $$(".only-auth").forEach(function (elemento) {
    elemento.classList.toggle("hidden", !estaLogueado());
  });

  $$(".only-seller").forEach(function (elemento) {
    elemento.classList.toggle("hidden", !tieneRol("SELLER"));
  });

  $$(".only-admin").forEach(function (elemento) {
    elemento.classList.toggle("hidden", !tieneRol("ADMIN"));
  });

  renderizarSubastas(subastasCache);
}

function cargarDatosPublicos() {
  cargarSubastas();
  cargarProductos();
  cargarCategorias();
}

function cargarSubastas() {
  return api("/api/auctions")
    .then(function (subastas) {
      subastasCache = subastas || [];
      renderizarSubastas(subastasCache);
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar las subastas: " + obtenerMensajeError(error), "error");
    });
}

function cargarProductos() {
  return api("/api/products")
    .then(function (productos) {
      productosCache = productos || [];
      renderizarProductos(productosCache);
      cargarSelects();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar los productos: " + obtenerMensajeError(error), "error");
    });
}

function cargarCategorias() {
  return api("/api/categories")
    .then(function (categorias) {
      categoriasCache = categorias || [];
      renderizarCategorias(categoriasCache);
      cargarSelects();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar las categorías: " + obtenerMensajeError(error), "error");
    });
}

function cargarSelects() {
  var selectCategoriaProducto = $("#productCategorySelect");
  var selectProductoSubasta = $("#auctionProductSelect");

  if (selectCategoriaProducto) {
    if (categoriasCache.length === 0) {
      selectCategoriaProducto.innerHTML = "<option value=''>No hay categorías cargadas</option>";
    } else {
      selectCategoriaProducto.innerHTML = categoriasCache.map(function (categoria) {
        return "<option value='" + categoria.id + "'>" + escaparHTML(categoria.name) + "</option>";
      }).join("");
    }
  }

  if (selectProductoSubasta) {
    if (productosCache.length === 0) {
      selectProductoSubasta.innerHTML = "<option value=''>No hay productos cargados</option>";
    } else {
      selectProductoSubasta.innerHTML = productosCache.map(function (producto) {
        return "<option value='" + producto.id + "'>#" + producto.id + " - " + escaparHTML(producto.name) + "</option>";
      }).join("");
    }
  }
}

function renderizarSubastas(subastas) {
  var contenedor = $("#auctionList");

  if (!contenedor) {
    return;
  }

  if (!subastas || subastas.length === 0) {
    contenedor.innerHTML = "<div class='empty'>Todavía no hay subastas cargadas.</div>";
    return;
  }

  var html = "";

  subastas.forEach(function (subasta) {
    var estado = String(subasta.status || "");
    var puedePublicar = tieneRol("SELLER") && estado === "BORRADOR";
    var puedeCancelar = estaLogueado() && (tieneRol("SELLER") || tieneRol("ADMIN")) && estado !== "FINALIZADA" && estado !== "ADJUDICADA" && estado !== "CANCELADA";

    html += "<article class='card'>";
    html += "<span class='badge " + escaparHTML(estado) + "'>" + escaparHTML(estado) + "</span>";
    html += "<h3>" + escaparHTML(subasta.productName) + "</h3>";
    html += "<p class='meta'>Subasta #" + subasta.id + " · Producto #" + subasta.productId + "</p>";
    html += "<p class='price'>" + formatearPrecio(subasta.currentPrice) + "</p>";
    html += "<p class='meta'>Precio base: " + formatearPrecio(subasta.basePrice) + "</p>";
    html += "<p class='meta'>Incremento mínimo: " + formatearPrecio(subasta.minimumIncrement) + "</p>";
    html += "<p class='meta'>Inicio: " + formatearFecha(subasta.startDate) + "</p>";
    html += "<p class='meta'>Cierre: " + formatearFecha(subasta.endDate) + "</p>";
    html += "<p>" + escaparHTML(subasta.description || "Sin descripción") + "</p>";
    html += "<div class='card-actions'>";

    html += "<button class='btn secondary small' data-action='detail' data-id='" + subasta.id + "'>Ver detalle</button>";

    if (tieneRol("USER") && estado === "ACTIVA") {
      html += "<button class='btn primary small' data-action='bid' data-id='" + subasta.id + "'>Pujar</button>";
    } else if (tieneRol("USER") && estado !== "ACTIVA") {
      html += "<button class='btn secondary small' disabled>Puja no disponible</button>";
}

    if (puedePublicar) {
      html += "<button class='btn primary small' data-action='publish' data-id='" + subasta.id + "'>Publicar</button>";
    }

    if (puedeCancelar) {
      html += "<button class='btn danger small' data-action='cancel' data-id='" + subasta.id + "'>Cancelar</button>";
    }

    if (estaLogueado()) {
      html += "<button class='btn secondary small' data-action='myBids' data-id='" + subasta.id + "'>Mis pujas</button>";
    }

    if (tieneRol("SELLER") || tieneRol("ADMIN")) {
      html += "<button class='btn secondary small' data-action='allBids' data-id='" + subasta.id + "'>Ver pujas</button>";
    }

    if (tieneRol("ADMIN")) {
      html += "<button class='btn secondary small' data-action='history' data-id='" + subasta.id + "'>Historial</button>";
    }

    html += "</div>";
    html += "</article>";
  });

  contenedor.innerHTML = html;
}

function renderizarProductos(productos) {
  var contenedor = $("#productList");

  if (!contenedor) {
    return;
  }

  if (!productos || productos.length === 0) {
    contenedor.innerHTML = "<div class='empty'>Todavía no hay productos cargados.</div>";
    return;
  }

  var html = "";

  productos.forEach(function (producto) {
    html += "<article class='card'>";
    html += "<h3>" + escaparHTML(producto.name) + "</h3>";
    html += "<p>" + escaparHTML(producto.description || "Sin descripción") + "</p>";
    html += "<p class='meta'>Producto #" + producto.id + "</p>";
    html += "<p class='meta'>Categoría: " + escaparHTML(producto.categoryName) + "</p>";
    html += "<p class='meta'>Vendedor: " + escaparHTML(producto.sellerUsername) + "</p>";
    html += "</article>";
  });

  contenedor.innerHTML = html;
}

function renderizarCategorias(categorias) {
  var contenedor = $("#categoryList");

  if (!contenedor) {
    return;
  }

  if (!categorias || categorias.length === 0) {
    contenedor.innerHTML = "<div class='empty'>Todavía no hay categorías cargadas.</div>";
    return;
  }

  var html = "";

  categorias.forEach(function (categoria) {
    html += "<article class='card'>";
    html += "<h3>" + escaparHTML(categoria.name) + "</h3>";
    html += "<p>" + escaparHTML(categoria.description || "Sin descripción") + "</p>";
    html += "<p class='meta'>Categoría #" + categoria.id + "</p>";
    html += "</article>";
  });

  contenedor.innerHTML = html;
}

function mostrarDetalleSubasta(id) {
  api("/api/auctions/" + id)
    .then(function (subasta) {
      var detalle = $("#auctionDetail");

      if (!detalle) {
        return;
      }

      detalle.classList.remove("hidden");
      detalle.innerHTML = "" +
        "<h3>Detalle de subasta #" + subasta.id + "</h3>" +
        "<p><strong>Producto:</strong> " + escaparHTML(subasta.productName) + "</p>" +
        "<p><strong>Vendedor:</strong> " + escaparHTML(subasta.sellerUsername) + "</p>" +
        "<p><strong>Estado:</strong> " + escaparHTML(subasta.status) + "</p>" +
        "<p><strong>Precio actual:</strong> " + formatearPrecio(subasta.currentPrice) + "</p>" +
        "<p><strong>Precio base:</strong> " + formatearPrecio(subasta.basePrice) + "</p>" +
        "<p><strong>Incremento mínimo:</strong> " + formatearPrecio(subasta.minimumIncrement) + "</p>" +
        "<p><strong>Inicio:</strong> " + formatearFecha(subasta.startDate) + "</p>" +
        "<p><strong>Cierre:</strong> " + formatearFecha(subasta.endDate) + "</p>" +
        "<p><strong>Ganador:</strong> " + escaparHTML(subasta.winnerUsername || "Todavía no visible") + "</p>" +
        "<p>" + escaparHTML(subasta.description || "Sin descripción") + "</p>";

      detalle.scrollIntoView({ behavior: "smooth", block: "start" });
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo cargar el detalle: " + obtenerMensajeError(error), "error");
    });
}

function publicarSubasta(id) {
  api("/api/auctions/" + id + "/publish", { method: "PATCH" })
    .then(function () {
      mostrarMensaje("Subasta publicada correctamente");
      cargarSubastas();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo publicar: " + obtenerMensajeError(error), "error");
    });
}

function cancelarSubasta(id) {
  var motivo = prompt("Indicá el motivo de cancelación:");

  if (!motivo) {
    return;
  }

  api("/api/auctions/" + id + "/cancel", {
    method: "PATCH",
    body: JSON.stringify({ reason: motivo })
  })
    .then(function () {
      mostrarMensaje("Subasta cancelada correctamente");
      cargarSubastas();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo cancelar: " + obtenerMensajeError(error), "error");
    });
}

function pujarSubasta(id) {
  if (!estaLogueado()) {
    mostrarMensaje("Para pujar primero tenés que iniciar sesión.", "error");
    cambiarSeccion("sectionAuth");
    return;
  }

  if (!tieneRol("USER")) {
    mostrarMensaje("Para pujar tenés que estar logueado con un usuario USER. El vendedor no puede pujar en su propia subasta.", "error");
    return;
  }

  var subasta = subastasCache.find(function (item) {
    return Number(item.id) === Number(id);
  });

  if (!subasta) {
    mostrarMensaje("No se encontró la subasta. Actualizá el listado e intentá de nuevo.", "error");
    return;
  }

  if (String(subasta.status || "") !== "ACTIVA") {
    mostrarMensaje("Solo se puede pujar cuando la subasta está ACTIVA. Si está PUBLICADA, esperá a que llegue la fecha de inicio y tocá Actualizar.", "error");
    return;
  }

  var textoMonto = prompt(
    "Ingresá el monto de la puja:\n" +
    "Precio actual: " + formatearPrecio(subasta.currentPrice) + "\n" +
    "Incremento mínimo: " + formatearPrecio(subasta.minimumIncrement)
  );

  if (!textoMonto) {
    return;
  }

  var monto = Number(String(textoMonto).replace(",", "."));

  if (isNaN(monto) || monto <= 0) {
    mostrarMensaje("El monto de la puja debe ser un número mayor a cero.", "error");
    return;
  }

  api("/api/auctions/" + id + "/bids", {
    method: "POST",
    body: JSON.stringify({
      auctionId: Number(id),
      amount: monto
    })
  })
    .then(function () {
      mostrarMensaje("Puja realizada correctamente");
      cargarSubastas();
      cargarMisPujas(id);
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo pujar: " + obtenerMensajeError(error), "error");
    });
}

function cargarMisPujas(idSubasta) {
  api("/api/auctions/" + idSubasta + "/bids/my")
    .then(function (pujas) {
      mostrarListadoEnDetalle("Mis pujas en subasta #" + idSubasta, renderizarPujas(pujas));
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar tus pujas: " + obtenerMensajeError(error), "error");
    });
}

function cargarTodasLasPujas(idSubasta) {
  api("/api/auctions/" + idSubasta + "/bids")
    .then(function (pujas) {
      mostrarListadoEnDetalle("Pujas de subasta #" + idSubasta, renderizarPujas(pujas));
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar las pujas: " + obtenerMensajeError(error), "error");
    });
}

function renderizarPujas(pujas) {
  if (!pujas || pujas.length === 0) {
    return "<div class='empty'>No hay pujas para mostrar.</div>";
  }

  var html = "<div class='list'>";

  pujas.forEach(function (puja) {
    html += "<div class='list-item'>";
    html += "<strong>" + formatearPrecio(puja.amount) + "</strong>";
    html += "<p class='meta'>Usuario: " + escaparHTML(puja.username) + "</p>";
    html += "<p class='meta'>Fecha: " + formatearFecha(puja.bidDate) + "</p>";
    html += "</div>";
  });

  html += "</div>";
  return html;
}

function cargarHistorial(idSubasta) {
  api("/api/auctions/" + idSubasta + "/history")
    .then(function (historial) {
      var html = "";

      if (!historial || historial.length === 0) {
        html = "<div class='empty'>No hay historial para mostrar.</div>";
      } else {
        html = "<div class='list'>";
        historial.forEach(function (item) {
          html += "<div class='list-item'>";
          html += "<strong>" + escaparHTML(item.previousState) + " → " + escaparHTML(item.newState) + "</strong>";
          html += "<p class='meta'>Fecha: " + formatearFecha(item.changeDate) + "</p>";
          html += "<p class='meta'>Responsable: " + escaparHTML(item.responsibleUsername) + "</p>";
          html += "<p>" + escaparHTML(item.reason || "Sin motivo") + "</p>";
          html += "</div>";
        });
        html += "</div>";
      }

      mostrarListadoEnDetalle("Historial de subasta #" + idSubasta, html);
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo cargar el historial: " + obtenerMensajeError(error), "error");
    });
}

function mostrarListadoEnDetalle(titulo, contenidoHtml) {
  var detalle = $("#auctionDetail");

  if (!detalle) {
    return;
  }

  detalle.classList.remove("hidden");
  detalle.innerHTML = "<h3>" + escaparHTML(titulo) + "</h3>" + contenidoHtml;
  detalle.scrollIntoView({ behavior: "smooth", block: "start" });
}

function cargarNotificaciones() {
  api("/api/notifications")
    .then(function (notificaciones) {
      var contenedor = $("#notificationList");

      if (!contenedor) {
        return;
      }

      if (!notificaciones || notificaciones.length === 0) {
        contenedor.innerHTML = "<div class='empty'>No tenés notificaciones.</div>";
        return;
      }

      var html = "";

      notificaciones.forEach(function (notificacion) {
        html += "<div class='list-item'>";
        html += "<strong>" + (notificacion.isRead ? "Leída" : "Pendiente") + "</strong>";
        html += "<p>" + escaparHTML(notificacion.message) + "</p>";
        html += "<p class='meta'>" + formatearFecha(notificacion.creationDate) + "</p>";

        if (!notificacion.isRead) {
          html += "<button class='btn secondary small' data-action='readNotification' data-id='" + notificacion.id + "'>Marcar como leída</button>";
        }

        html += "</div>";
      });

      contenedor.innerHTML = html;
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar las notificaciones: " + obtenerMensajeError(error), "error");
    });
}

function marcarNotificacionLeida(id) {
  api("/api/notifications/" + id + "/read", { method: "PATCH" })
    .then(function () {
      mostrarMensaje("Notificación marcada como leída");
      cargarNotificaciones();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo marcar la notificación: " + obtenerMensajeError(error), "error");
    });
}

function cargarMisReclamos() {
  api("/api/disputes/my")
    .then(function (reclamos) {
      renderizarReclamos(reclamos, $("#myDisputeList"));
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar tus reclamos: " + obtenerMensajeError(error), "error");
    });
}

function renderizarReclamos(reclamos, contenedor) {
  if (!contenedor) {
    return;
  }

  if (!reclamos || reclamos.length === 0) {
    contenedor.innerHTML = "<div class='empty'>No hay reclamos para mostrar.</div>";
    return;
  }

  var html = "";

  reclamos.forEach(function (reclamo) {
    html += "<div class='list-item'>";
    html += "<strong>Reclamo #" + reclamo.id + " - Subasta #" + reclamo.auctionId + "</strong>";
    html += "<p class='meta'>Usuario: " + escaparHTML(reclamo.initiatorUsername) + "</p>";
    html += "<p class='meta'>Fecha: " + formatearFecha(reclamo.creationDate) + "</p>";
    html += "<p><strong>Motivo:</strong> " + escaparHTML(reclamo.reason) + "</p>";
    html += "<p>" + escaparHTML(reclamo.description) + "</p>";
    html += "<p><strong>Resolución:</strong> " + escaparHTML(reclamo.adminResolution || "Pendiente") + "</p>";
    html += "</div>";
  });

  contenedor.innerHTML = html;
}

function cargarDatosAdmin() {
  api("/api/users")
    .then(function (usuarios) {
      renderizarUsuariosAdmin(usuarios);
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar los usuarios: " + obtenerMensajeError(error), "error");
    });

  api("/api/disputes")
    .then(function (reclamos) {
      renderizarReclamos(reclamos, $("#adminDisputeList"));
    })
    .catch(function (error) {
      mostrarMensaje("No se pudieron cargar los reclamos: " + obtenerMensajeError(error), "error");
    });
}

function renderizarUsuariosAdmin(usuarios) {
  var contenedor = $("#adminUserList");

  if (!contenedor) {
    return;
  }

  if (!usuarios || usuarios.length === 0) {
    contenedor.innerHTML = "<div class='empty'>No hay usuarios para mostrar.</div>";
    return;
  }

  var html = "";

  usuarios.forEach(function (usuario) {
    html += "<div class='list-item'>";
    html += "<strong>" + escaparHTML(usuario.username) + "</strong>";
    html += "<p class='meta'>" + escaparHTML(usuario.email) + "</p>";
    html += "<p class='meta'>Roles: " + escaparHTML((usuario.roles || []).join(", ")) + "</p>";
    html += "<p class='meta'>Estado: " + (usuario.isBlocked ? "Bloqueado" : "Activo") + "</p>";

    if (usuario.isBlocked) {
      html += "<button class='btn secondary small' data-action='unblockUser' data-id='" + usuario.id + "'>Desbloquear</button>";
    } else {
      html += "<button class='btn danger small' data-action='blockUser' data-id='" + usuario.id + "'>Bloquear</button>";
    }

    html += "</div>";
  });

  contenedor.innerHTML = html;
}

function bloquearUsuario(id) {
  api("/api/users/" + id + "/block", { method: "PATCH" })
    .then(function () {
      mostrarMensaje("Usuario bloqueado");
      cargarDatosAdmin();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo bloquear: " + obtenerMensajeError(error), "error");
    });
}

function desbloquearUsuario(id) {
  api("/api/users/" + id + "/unblock", { method: "PATCH" })
    .then(function () {
      mostrarMensaje("Usuario desbloqueado");
      cargarDatosAdmin();
    })
    .catch(function (error) {
      mostrarMensaje("No se pudo desbloquear: " + obtenerMensajeError(error), "error");
    });
}

function configurarEventos() {
  $$(".nav-btn").forEach(function (boton) {
    boton.addEventListener("click", function () {
      cambiarSeccion(boton.dataset.section);
    });
  });

  var botonLogout = $("#logoutBtn");
  if (botonLogout) {
    botonLogout.addEventListener("click", function () {
      cerrarSesion();
      mostrarMensaje("Sesión cerrada correctamente");
      cambiarSeccion("sectionAuctions");
    });
  }

  var botonActualizar = $("#refreshBtn");
  if (botonActualizar) {
    botonActualizar.addEventListener("click", function () {
      botonActualizar.disabled = true;
      botonActualizar.textContent = "Actualizando...";

      Promise.all([
        cargarSubastas(),
        cargarProductos(),
        cargarCategorias()
    ])
      .then(function () {
        mostrarMensaje("Datos actualizados correctamente");
      })
      .finally(function () {
        botonActualizar.disabled = false;
        botonActualizar.textContent = "Actualizar";
      });
  });
}

  var loginForm = $("#loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var datos = new FormData(loginForm);

      api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
          identifier: datos.get("identifier"),
          password: datos.get("password")
        })
      })
        .then(function (respuesta) {
          subastasCache = [];
          productosCache = [];
          categoriasCache = [];

          limpiarPanelesPrivados();

          guardarSesion(respuesta);
          mostrarMensaje("Inicio de sesión correcto");
          loginForm.reset();
          cargarDatosPublicos();
          cambiarSeccion("sectionAuctions");
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo iniciar sesión: " + obtenerMensajeError(error), "error");
        });
    });
  }

  var registerForm = $("#registerForm");
  if (registerForm) {
    registerForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var datos = new FormData(registerForm);

      api("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          username: datos.get("username"),
          email: datos.get("email"),
          password: datos.get("password"),
          role: datos.get("role")
        })
      })
        .then(function (respuesta) {
          subastasCache = [];
          productosCache = [];
          categoriasCache = [];

          limpiarPanelesPrivados();

          guardarSesion(respuesta);
          mostrarMensaje("Usuario registrado correctamente");
          registerForm.reset();
          cargarDatosPublicos();
          cambiarSeccion("sectionAuctions");
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo registrar: " + obtenerMensajeError(error), "error");
        });
    });
  }

  var categoryForm = $("#categoryForm");
  if (categoryForm) {
    categoryForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var datos = new FormData(categoryForm);

      api("/api/categories", {
        method: "POST",
        body: JSON.stringify({
          name: datos.get("name"),
          description: datos.get("description")
        })
      })
        .then(function () {
          mostrarMensaje("Categoría creada correctamente");
          categoryForm.reset();
          cargarCategorias();
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo crear la categoría: " + obtenerMensajeError(error), "error");
        });
    });
  }

  var productForm = $("#productForm");
  if (productForm) {
    productForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var datos = new FormData(productForm);

      api("/api/products", {
        method: "POST",
        body: JSON.stringify({
          name: datos.get("name"),
          description: datos.get("description"),
          categoryId: Number(datos.get("categoryId"))
        })
      })
        .then(function () {
          mostrarMensaje("Producto creado correctamente");
          productForm.reset();
          cargarProductos();
          cambiarSeccion("sectionProducts");
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo crear el producto: " + obtenerMensajeError(error), "error");
        });
    });
  }

  var auctionForm = $("#auctionForm");
  if (auctionForm) {
    configurarFechasSubasta();

    auctionForm.addEventListener("submit", function (event) {
      event.preventDefault();

      var datos = new FormData(auctionForm);

      var productId = Number(datos.get("productId"));
      var basePrice = Number(datos.get("basePrice"));
      var minimumIncrement = Number(datos.get("minimumIncrement"));

      var startDate = datos.get("startDate");
      var endDate = datos.get("endDate");

      var inicio = new Date(startDate);
      var cierre = new Date(endDate);

      var ahora = new Date();
      var inicioMinimo = new Date(ahora.getTime() + 60000);

      if (!productId) {
        mostrarMensaje("Debés seleccionar un producto.", "error");
        return;
      }

      if (isNaN(basePrice) || basePrice < 0) {
        mostrarMensaje("El precio base debe ser cero o mayor.", "error");
        return;
      }

      if (isNaN(minimumIncrement) || minimumIncrement <= 0) {
        mostrarMensaje("El incremento mínimo debe ser mayor a cero.", "error");
        return;
      }

      if (!startDate || !endDate) {
        mostrarMensaje("Debés completar la fecha de inicio y la fecha de cierre.", "error");
        return;
      }

      if (isNaN(inicio.getTime()) || isNaN(cierre.getTime())) {
        mostrarMensaje("Las fechas ingresadas no son válidas.", "error");
        return;
      }

      if (inicio < inicioMinimo) {
        mostrarMensaje("La fecha de inicio debe ser al menos 1 minuto posterior a la hora actual.", "error");
        return;
      }

      if (cierre <= inicio) {
        mostrarMensaje("La fecha de cierre debe ser posterior a la fecha de inicio.", "error");
        return;
      }

      api("/api/auctions", {
        method: "POST",
        body: JSON.stringify({
          productId: productId,
          basePrice: basePrice,
          minimumIncrement: minimumIncrement,
          startDate: fechaLocalAIso(startDate),
          endDate: fechaLocalAIso(endDate),
          description: datos.get("description")
        })
      })
        .then(function () {
          mostrarMensaje("Subasta creada correctamente. Ahora podés publicarla desde el listado.");
          auctionForm.reset();
          configurarFechasSubasta();
          cargarSubastas();
          cambiarSeccion("sectionAuctions");
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo crear la subasta: " + obtenerMensajeError(error), "error");
        });
    });
  }

  var auctionList = $("#auctionList");
  if (auctionList) {
    auctionList.addEventListener("click", function (event) {
      var boton = event.target.closest("button[data-action]");
      if (!boton) {
        return;
      }

      var accion = boton.dataset.action;
      var id = boton.dataset.id;

      if (accion === "detail") mostrarDetalleSubasta(id);
      if (accion === "bid") pujarSubasta(id);
      if (accion === "publish") publicarSubasta(id);
      if (accion === "cancel") cancelarSubasta(id);
      if (accion === "myBids") cargarMisPujas(id);
      if (accion === "allBids") cargarTodasLasPujas(id);
      if (accion === "history") cargarHistorial(id);
    });
  }

  var botonNotificaciones = $("#loadNotificationsBtn");
  if (botonNotificaciones) {
    botonNotificaciones.addEventListener("click", cargarNotificaciones);
  }

  var notificationList = $("#notificationList");
  if (notificationList) {
    notificationList.addEventListener("click", function (event) {
      var boton = event.target.closest("button[data-action='readNotification']");
      if (boton) {
        marcarNotificacionLeida(boton.dataset.id);
      }
    });
  }

  var botonMisReclamos = $("#loadMyDisputesBtn");
  if (botonMisReclamos) {
    botonMisReclamos.addEventListener("click", cargarMisReclamos);
  }

  var disputeForm = $("#disputeForm");
  if (disputeForm) {
    disputeForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var datos = new FormData(disputeForm);
      var auctionId = Number(datos.get("auctionId"));

      api("/api/auctions/" + auctionId + "/disputes", {
        method: "POST",
        body: JSON.stringify({
          auctionId: auctionId,
          reason: datos.get("reason"),
          description: datos.get("description")
        })
      })
        .then(function () {
          mostrarMensaje("Reclamo abierto correctamente");
          disputeForm.reset();
          cargarMisReclamos();
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo abrir el reclamo: " + obtenerMensajeError(error), "error");
        });
    });
  }

  var botonAdmin = $("#loadAdminBtn");
  if (botonAdmin) {
    botonAdmin.addEventListener("click", cargarDatosAdmin);
  }

  var adminUserList = $("#adminUserList");
  if (adminUserList) {
    adminUserList.addEventListener("click", function (event) {
      var boton = event.target.closest("button[data-action]");
      if (!boton) {
        return;
      }

      if (boton.dataset.action === "blockUser") bloquearUsuario(boton.dataset.id);
      if (boton.dataset.action === "unblockUser") desbloquearUsuario(boton.dataset.id);
    });
  }

  var resolveDisputeForm = $("#resolveDisputeForm");
  if (resolveDisputeForm) {
    resolveDisputeForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var datos = new FormData(resolveDisputeForm);

      api("/api/disputes/" + datos.get("disputeId") + "/resolve", {
        method: "PATCH",
        body: JSON.stringify({
          adminResolution: datos.get("adminResolution"),
          newAuctionStatus: datos.get("newAuctionStatus")
        })
      })
        .then(function () {
          mostrarMensaje("Reclamo resuelto correctamente");
          resolveDisputeForm.reset();
          cargarDatosAdmin();
          cargarSubastas();
        })
        .catch(function (error) {
          mostrarMensaje("No se pudo resolver el reclamo: " + obtenerMensajeError(error), "error");
        });
    });
  }
}

function iniciar() {
  configurarEventos();
  renderizarSesion();
  cambiarSeccion("sectionAuctions");
  cargarDatosPublicos();
}

document.addEventListener("DOMContentLoaded", iniciar);