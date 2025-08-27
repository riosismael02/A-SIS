document.addEventListener("DOMContentLoaded", function () {
    const inputEmpleado = document.getElementById("empleado");
    const inputDni = document.getElementById("dni") || document.getElementById("dniBusqueda");
    const sugerencias = document.getElementById("sugerencias");

    inputEmpleado.addEventListener("input", function () {
        const query = this.value.trim();

        if (query.length < 2) {
            sugerencias.innerHTML = "";
            sugerencias.style.display = "none"; // Ocultar si no hay suficientes caracteres
            return;
        }

        fetch(`/empleados/buscar?q=${encodeURIComponent(query)}`)
            .then(response => response.json())
            .then(data => {
                sugerencias.innerHTML = "";

                if (data.length > 0) {
                    data.forEach(e => sugerencias.appendChild(crearSugerenciaEmpleado(e)));
                    sugerencias.style.display = "block"; // Mostrar sugerencias
                } else {
                    sugerencias.style.display = "none"; // Ocultar si no hay resultados
                }
            })
            .catch(error => {
                console.error("Error al buscar empleados:", error);
                sugerencias.innerHTML = "";
                sugerencias.style.display = "none";
            });
    });

    function crearSugerenciaEmpleado(e) {
        const item = document.createElement("a");
        item.href = "#";
        item.className = "list-group-item list-group-item-action";
        item.dataset.dni = e.dni;
        item.textContent = `${e.apellido} ${e.nombre} - ${e.dni}`;

        item.addEventListener("click", function (event) {
            event.preventDefault();
            inputEmpleado.value = this.textContent;
            if (inputDni) inputDni.value = this.dataset.dni;
            sugerencias.innerHTML = "";
            sugerencias.style.display = "none";

            // Solo hacemos fetch si existe el formulario de edición
            if (document.getElementById("formEditarEmpleado")) {
                fetch(`/empleados/${this.dataset.dni}`)
                    .then(response => response.json())
                    .then(e => setearDatosEmpleado(e))
                    .catch(error => {
                        console.error("Error al obtener datos del empleado:", error);
                    });
            }
        });

        return item;
    }

    function setearDatosEmpleado(e) {
        const nombreInput = document.getElementById("nombre");
        const apellidoInput = document.getElementById("apellido");
        const dniInput = document.getElementById("dni");
        const horaEntradaInput = document.getElementById("horaEntrada");
        const horaSalidaInput = document.getElementById("horaSalida");
        const flexMinutosInput = document.getElementById("flexMinutos");
        const hiddenIdInput = document.querySelector("#formEditarEmpleado input[name='id']");
        const areaSelect = document.getElementById("area");
        const tipoContratoSelect = document.getElementById("tipoContrato");
        const horaEntrada2Input = document.getElementById("horaEntrada2");
        const horaSalida2Input = document.getElementById("horaSalida2");

        if (nombreInput) nombreInput.value = e.nombre || "";
        if (apellidoInput) apellidoInput.value = e.apellido || "";
        if (dniInput) dniInput.value = e.dni || "";
        if (horaEntradaInput) horaEntradaInput.value = e.horaEntrada || "";
        if (horaSalidaInput) horaSalidaInput.value = e.horaSalida || "";
        if (horaEntrada2Input) horaEntrada2Input.value = e.horaEntrada2 || "";
        if (horaSalida2Input) horaSalida2Input.value = e.horaSalida2 || "";
        if (flexMinutosInput) flexMinutosInput.value = e.flexMinutos || "";
        if (hiddenIdInput) hiddenIdInput.value = e.id || "";

        if (areaSelect && e.area && e.area.id) {
            areaSelect.value = e.area.id;
        }

        if (tipoContratoSelect && e.tipoContrato) {
            tipoContratoSelect.value = e.tipoContrato;
        }
    }

    // Ocultar sugerencias si hacés clic fuera
    document.addEventListener("click", function (event) {
        if (!sugerencias.contains(event.target) && event.target !== inputEmpleado) {
            sugerencias.innerHTML = "";
            sugerencias.style.display = "none";
        }
    });

    // Ocultar sugerencias al presionar ESC
    inputEmpleado.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            sugerencias.innerHTML = "";
            sugerencias.style.display = "none";
        }
    });
});