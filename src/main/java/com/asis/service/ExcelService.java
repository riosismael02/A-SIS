package com.asis.service;

import com.asis.model.Empleado;
import com.asis.model.LogCargaAsistencia;
import com.asis.model.Marca;
import com.asis.model.RegistroAsistencia;
import com.asis.model.dto.CargaAsistenciaDTO;
import com.asis.model.dto.EdicionHorarioDTO;
import com.asis.model.dto.ResumenAsistenciasDTO;
import com.asis.model.dto.ResumenEmpleadoDTO;
import com.asis.repository.EmpleadoRepository;
import com.asis.repository.LogCargaAsistenciaRepository;
import com.asis.repository.RegistroRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final EmpleadoRepository empleadoRepo;
    private final RegistroRepository registroRepository;

    private final LogCargaAsistenciaRepository logCargaRepository;
    private final FeriadoService feriadoService;


    @Transactional
    public void cargarYGuardarAsistencias(CargaAsistenciaDTO dto) {
        // 1. Extraer marcas válidas
        List<Marca> marcasValidas = extraerMarcasValidas(dto.getArchivo(), dto.getDesde(), dto.getHasta());

        // 2. Convertir a registros de asistencia
        List<RegistroAsistencia> registros = convertirMarcasARegistros(marcasValidas);

        // 3. Guardar registros
        registroRepository.saveAll(registros);

        // 4. Generar descripción automática
        String descripcion = generarDescripcion(dto.getDesde(), dto.getHasta());
        dto.setDescripcion(descripcion);

        // 5. Registrar log de carga
        LogCargaAsistencia log = new LogCargaAsistencia();
        log.setDesde(dto.getDesde());
        log.setHasta(dto.getHasta());
        log.setDescripcion(descripcion);
        log.setFechaCarga(LocalDateTime.now());
        log.setCantidadRegistros(registros.size());

        logCargaRepository.save(log);
    }

    public String generarDescripcion(LocalDate desde, LocalDate hasta) {
        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("MMMM", new Locale("es", "ES"));
        String mes = desde.format(mesFormatter);
        return "Mes de " + capitalize(mes) + " " + desde.getYear();
    }

    private String capitalize(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    public List<Marca> extraerMarcasValidas(MultipartFile archivo, LocalDate desde, LocalDate hasta) {
        List<Marca> todas = leerExcel(archivo);
        return todas.stream()
                .filter(m -> !m.getFecha().isBefore(desde) && !m.getFecha().isAfter(hasta))
                .filter(m -> empleadoRepo.findByDni(m.getDni()).isPresent())
                .toList();
    }

    private List<Marca> leerExcel(MultipartFile file) {
        List<Marca> marcas = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Saltar encabezado

                // Leer columna 3 (índice 2) como DNI (EnNo)
                Cell cellDni = row.getCell(2);
                String dni = obtenerDniComoTexto(cellDni);

                // Leer columna 10 (índice 9) como fecha y hora
                Cell cellFechaHora = row.getCell(9);
                if (cellFechaHora == null || cellFechaHora.getCellType() != CellType.NUMERIC || !DateUtil.isCellDateFormatted(cellFechaHora)) {
                    continue; // saltar si no es fecha válida
                }

                LocalDateTime fechaHora = cellFechaHora.getLocalDateTimeCellValue();
                marcas.add(new Marca(dni, fechaHora.toLocalDate(), fechaHora.toLocalTime()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo Excel", e);
        }

        return marcas;
    }

    private String obtenerDniComoTexto(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else {
            return cell.getStringCellValue().trim();
        }
    }

    public List<RegistroAsistencia> convertirMarcasARegistros(List<Marca> marcas) {
        Map<String, List<Marca>> marcasPorDni = marcas.stream()
                .sorted(Comparator.comparing(Marca::getFecha).thenComparing(Marca::getHora))
                .collect(Collectors.groupingBy(Marca::getDni));

        List<RegistroAsistencia> registros = new ArrayList<>();

        for (var entry : marcasPorDni.entrySet()) {
            String dni = entry.getKey();
            Empleado emp = empleadoRepo.findByDni(dni).orElseThrow();
            Map<LocalDate, List<Marca>> porDia = entry.getValue().stream()
                    .collect(Collectors.groupingBy(Marca::getFecha));

            for (var dia : porDia.entrySet()) {
                List<Marca> delDia = dia.getValue();
                for (int i = 0; i < delDia.size(); i++) {
                    Marca marca = delDia.get(i);
                    RegistroAsistencia reg = new RegistroAsistencia();
                    reg.setEmpleado(emp);
                    reg.setFecha(marca.getFecha());
                    reg.setHora(marca.getHora());
                    reg.setOrdenDia(i + 1);
                    reg.setTipoHora(detectarTipoHora(emp, marca.getFecha(), i));
                    registros.add(reg);
                }
            }
        }
        return registros;
    }


    public List<ResumenEmpleadoDTO> procesarAsistenciasEmpleado(Empleado empleado, List<RegistroAsistencia> registros, LocalDate desde, LocalDate hasta) {
        Map<LocalDate, List<RegistroAsistencia>> registrosPorDia = registros.stream()
                .sorted(Comparator.comparing(RegistroAsistencia::getHora))
                .collect(Collectors.groupingBy(RegistroAsistencia::getFecha));

        List<ResumenEmpleadoDTO> detalle = new ArrayList<>();
        DateTimeFormatter formatoLatino = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Locale localeEs = new Locale("es", "ES");

        double totalHoras = 0;
        double totalNormales = 0;
        double totalExtras = 0;
        double totalFinde = 0;
        int totalAusencias = 0;
        int totalLlegadasTarde = 0;
        int diasTrabajados = 0;
        int minutosTardeTotales = 0;
        int totalIncompletos = 0;


        LocalDate actual = desde;

        while (!actual.isAfter(hasta)) {
            String nombreDia = actual.getDayOfWeek().getDisplayName(TextStyle.FULL, localeEs);
            String fechaFormateada = actual.format(formatoLatino);

            List<RegistroAsistencia> marcasDelDia = registrosPorDia.getOrDefault(actual, List.of()).stream()
                    .sorted(Comparator.comparing(RegistroAsistencia::getHora))
                    .toList();

            boolean esFeriado = feriadoService.esFeriado(actual);
            boolean esFinDeSemana = actual.getDayOfWeek() == DayOfWeek.SATURDAY || actual.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean esLaboral = !esFeriado && !esFinDeSemana;

            double horasJustificadasDia = 0;
            double horasRealesDia = 0;
            boolean tieneHorasJustificadas = false;
            boolean tieneHorasReales = false;
            boolean esPrimeraMarca = true;
            boolean tieneMarcasIncompletas = false;

            if (!marcasDelDia.isEmpty()) {
                if (marcasDelDia.size() % 2 != 0) {
                    totalIncompletos++;
                    tieneMarcasIncompletas = true;

                    RegistroAsistencia marcaIncompleta = marcasDelDia.get(marcasDelDia.size() - 1);
                    boolean faltaSalida = marcaIncompleta.getOrdenDia() % 2 == 1;

                    ResumenEmpleadoDTO dtoIncompleto = new ResumenEmpleadoDTO();
                    dtoIncompleto.setDni(empleado.getDni());
                    dtoIncompleto.setNombreCompleto(empleado.getNombre() + " " + empleado.getApellido());
                    dtoIncompleto.setFechaFormateada(fechaFormateada);
                    dtoIncompleto.setNombreDia(nombreDia);
                    dtoIncompleto.setMarcaIncompleta(true);
                    dtoIncompleto.setTipoIncompleto(faltaSalida ? "FALTA_SALIDA" : "FALTA_ENTRADA");
                    dtoIncompleto.setHoraEntrada(faltaSalida ? marcaIncompleta.getHora() : null);
                    dtoIncompleto.setHoraSalida(faltaSalida ? null : marcaIncompleta.getHora());
                    dtoIncompleto.setTipoHora("INCOMPLETO");
                    dtoIncompleto.setHorasTrabajadas(0);
                    dtoIncompleto.setAusente(false);
                    dtoIncompleto.setEsFeriado(esFeriado);
                    dtoIncompleto.setEsFinDeSemana(esFinDeSemana);
                    dtoIncompleto.setJustificada(false);

                    detalle.add(dtoIncompleto);
                }

                // Determinar horario activo (primer o segundo)
                LocalTime horaEntradaActiva = empleado.getHoraEntrada();
                LocalTime horaSalidaActiva = empleado.getHoraSalida();
                boolean esSegundoHorario = false;

                if (empleado.getHoraEntrada2() != null && empleado.getHoraSalida2() != null) {
                    RegistroAsistencia primeraEntrada = marcasDelDia.get(0);
                    long diffHorario1 = Math.abs(Duration.between(primeraEntrada.getHora(), empleado.getHoraEntrada()).toMinutes());
                    long diffHorario2 = Math.abs(Duration.between(primeraEntrada.getHora(), empleado.getHoraEntrada2()).toMinutes());
                    if (diffHorario2 < diffHorario1) {
                        horaEntradaActiva = empleado.getHoraEntrada2();
                        horaSalidaActiva = empleado.getHoraSalida2();
                        esSegundoHorario = true;
                    }
                }

                int flexMinutos = Optional.ofNullable(empleado.getFlexMinutos()).orElse(0);
                LocalTime horaEntradaMaxima = horaEntradaActiva.plusMinutes(flexMinutos);
                LocalTime horaEntradaMinima = horaEntradaActiva.minusMinutes(flexMinutos);
                LocalTime horaSalidaMinima = horaSalidaActiva.minusMinutes(flexMinutos);
                LocalTime horaSalidaMaxima = horaSalidaActiva.plusMinutes(flexMinutos);

                for (int i = 0; i + 1 < marcasDelDia.size(); i += 2) {
                    RegistroAsistencia entrada = marcasDelDia.get(i);
                    RegistroAsistencia salida = marcasDelDia.get(i + 1);

                    // CONVERTIR las fotos a Base64
                    String fotoEntradaBase64 = null;
                    if (entrada.getFoto() != null && entrada.getFoto().length > 0) {
                        fotoEntradaBase64 = Base64.getEncoder().encodeToString(entrada.getFoto());
                    }

                    String fotoSalidaBase64 = null;
                    if (salida.getFoto() != null && salida.getFoto().length > 0) {
                        fotoSalidaBase64 = Base64.getEncoder().encodeToString(salida.getFoto());
                    }
                    if (entrada.getHora().equals(salida.getHora()) || entrada.getHora().isAfter(salida.getHora())) {
                        continue;
                    }

                    double horas = Duration.between(entrada.getHora(), salida.getHora()).toMinutes() / 60.0;
                    boolean esJustificada = entrada.getJustificacion() != null;

                    boolean llegoTarde = false;
                    int minutosTarde = 0;
                    String tipoHora;
                    double horasNormales = 0;
                    double horasExtras = 0;
                    double horasFinde = 0;
                    double despuesDeHora = 0;

                    if (esFeriado || esFinDeSemana) {
                        tipoHora = esFeriado ? "FERIADO" : "FIN_SEMANA";
                        horasFinde = horas;
                        totalFinde += horasFinde;
                    } else {
                        if (i == 0 && entrada.getHora().isAfter(horaEntradaMaxima)) {
                            llegoTarde = true;
                            minutosTarde = (int) Duration.between(horaEntradaMaxima, entrada.getHora()).toMinutes();
                            totalLlegadasTarde++;
                            minutosTardeTotales += minutosTarde;
                        }

                        LocalTime inicioHorasNormales = entrada.getHora().isBefore(horaEntradaMinima)
                                ? horaEntradaMinima : entrada.getHora();
                        LocalTime finHorasNormales = salida.getHora().isAfter(horaSalidaMaxima)
                                ? horaSalidaMaxima : salida.getHora();

                        if (inicioHorasNormales.isBefore(finHorasNormales)) {
                            horasNormales = Duration.between(inicioHorasNormales, finHorasNormales).toMinutes() / 60.0;
                        }

                        if (entrada.getHora().isBefore(horaEntradaMinima)) {
                            double extrasEntrada = Duration.between(entrada.getHora(), horaEntradaMinima).toMinutes() / 60.0;
                            horasExtras += extrasEntrada;
                        }
                        if (salida.getHora().isAfter(horaSalidaMaxima)) {
                            double extrasSalida = Duration.between(horaSalidaMaxima, salida.getHora()).toMinutes() / 60.0;
                            horasExtras += extrasSalida;
                        }

                        tipoHora = esPrimeraMarca ? (horasExtras > 0 ? "MIXTO" : "NORMAL") : "EXTRA";

                        totalNormales += horasNormales;
                        totalExtras += horasExtras;

                        despuesDeHora = horasExtras * 60;
                    }

                    totalHoras += (horasNormales + horasExtras + horasFinde);

                    if (esJustificada) {
                        horasJustificadasDia += horas;
                        tieneHorasJustificadas = true;
                    } else {
                        horasRealesDia += (horasNormales + horasExtras + horasFinde);
                        tieneHorasReales = true;
                    }

                    ResumenEmpleadoDTO dto = new ResumenEmpleadoDTO();
                    dto.setDni(empleado.getDni());
                    dto.setNombreCompleto(empleado.getNombre() + " " + empleado.getApellido());
                    dto.setFechaFormateada(fechaFormateada);
                    dto.setNombreDia(nombreDia);
                    dto.setHoraEntrada(entrada.getHora());
                    dto.setHoraSalida(salida.getHora());
                    dto.setTipoHora(tipoHora);
                    dto.setHorasTrabajadas(horasNormales + horasExtras + horasFinde);
                    dto.setMarcaIncompleta(false);
                    dto.setAusente(false);
                    dto.setLlegoTarde(llegoTarde);
                    dto.setMinutosTarde(minutosTarde);
                    dto.setEsFeriado(esFeriado);
                    dto.setEsFinDeSemana(esFinDeSemana);
                    dto.setJustificada(esJustificada);
                    dto.setSegundoHorario(esSegundoHorario);
                    dto.setHorasExtras(horasExtras);
                    dto.setFotoEntradaBase64(fotoEntradaBase64);
                    dto.setFotoSalidaBase64(fotoSalidaBase64);

                    detalle.add(dto);

                    esPrimeraMarca = false;
                }

                boolean trabajoEseDia = tieneHorasReales || tieneHorasJustificadas || !marcasDelDia.isEmpty();
                if (trabajoEseDia && !tieneMarcasIncompletas) {
                    diasTrabajados++;
                }

            } else {
                boolean esAusencia = esLaboral && !tieneHorasJustificadas;

                ResumenEmpleadoDTO dto = new ResumenEmpleadoDTO();
                dto.setDni(empleado.getDni());
                dto.setNombreCompleto(empleado.getNombre() + " " + empleado.getApellido());
                dto.setFechaFormateada(fechaFormateada);
                dto.setNombreDia(nombreDia);
                dto.setTipoHora("AUSENTE");
                dto.setHorasTrabajadas(0);
                dto.setAusente(esAusencia);
                dto.setEsFeriado(esFeriado);
                dto.setEsFinDeSemana(esFinDeSemana);
                dto.setJustificada(false);
                dto.setLlegoTarde(false);
                dto.setMinutosTarde(0);
                dto.setMarcaIncompleta(false);
                dto.setSegundoHorario(false);

                detalle.add(dto);

                if (esAusencia) {
                    totalAusencias++;
                }
            }

            actual = actual.plusDays(1);
        }

        return detalle;
    }

    public Map<String, Object> generarDetalleEmpleadoView(String dni, LocalDate desde, LocalDate hasta) {
        Empleado empleado = empleadoRepo.findByDni(dni).orElseThrow();
        List<RegistroAsistencia> registros = registroRepository.findByEmpleadoDniAndFechaBetween(dni, desde, hasta);

        List<ResumenEmpleadoDTO> detalle = procesarAsistenciasEmpleado(empleado, registros, desde, hasta);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("detalle", detalle);
        resultado.put("empleado", empleado);
        resultado.put("desde", desde);
        resultado.put("hasta", hasta);
        resultado.put("dniSeleccionado", dni);

        // Filtramos registros válidos para los cálculos
        List<ResumenEmpleadoDTO> registrosValidos = detalle.stream()
                .filter(d -> !d.isMarcaIncompleta() &&
                        !"INVALIDO".equals(d.getTipoHora()))
                .toList();

        // Cálculo de totales (optimizado)
        double totalHoras = registrosValidos.stream()
                .mapToDouble(ResumenEmpleadoDTO::getHorasTrabajadas)
                .sum();

        double totalNormales = registrosValidos.stream()
                .mapToDouble(d -> {
                    if ("NORMAL".equals(d.getTipoHora())) {
                        return d.getHorasTrabajadas();
                    } else if ("MIXTO".equals(d.getTipoHora())) {
                        return d.getHorasTrabajadas() - (d.getDespuesDeHora() / 60.0);
                    }
                    return 0;
                })
                .sum();

        double totalExtras = registrosValidos.stream()
                .mapToDouble(ResumenEmpleadoDTO::getHorasExtras)
                .sum();

        double totalFinde = registrosValidos.stream()
                .filter(d -> "FIN_SEMANA".equals(d.getTipoHora()) || "FERIADO".equals(d.getTipoHora()))
                .mapToDouble(ResumenEmpleadoDTO::getHorasTrabajadas)
                .sum();

        // Total ausencias (solo días laborales)
        List<ResumenEmpleadoDTO> ausencias = detalle.stream()
                .filter(d -> {
                    boolean esAusente = d.isAusente();
                    boolean noEsFeriado = !d.isEsFeriado();
                    boolean noEsFinDeSemana = !d.isEsFinDeSemana();
                    System.out.println("Fecha: " + d.getFechaFormateada() +
                            " - Ausente: " + esAusente +
                            " - Feriado: " + d.isEsFeriado() +
                            " - FinSemana: " + d.isEsFinDeSemana());
                    return esAusente && noEsFeriado && noEsFinDeSemana;
                })
                .toList();

        long totalAusencias = ausencias.size();


        // Total llegadas tarde (solo días laborales, en cualquier horario)
        long totalLlegadasTarde = detalle.stream()
                .filter(d -> d.isLlegoTarde() &&
                        !d.isEsFeriado() &&
                        !d.isEsFinDeSemana())
                .count();

        // Días trabajados (solo días con marcas válidas)
        int diasTrabajados = (int) detalle.stream()
                .filter(d -> {
                    boolean tieneMarcas = d.getHoraEntrada() != null || d.getHoraSalida() != null;
                    boolean esValido = !d.isMarcaIncompleta() && !"INVALIDO".equals(d.getTipoHora());
                    return tieneMarcas && esValido;
                })
                .map(ResumenEmpleadoDTO::getFechaFormateada)
                .distinct()
                .count();

        int minutosTardeTotales = detalle.stream()
                .filter(d -> d.isLlegoTarde() && !d.isEsFeriado() && !d.isEsFinDeSemana())
                .mapToInt(ResumenEmpleadoDTO::getMinutosTarde) // corregido, no getMinutosTardeTotales()
                .sum();


        resultado.put("totalHoras", totalHoras);
        resultado.put("totalNormales", totalNormales);
        resultado.put("totalExtras", totalExtras);
        resultado.put("totalFindeFeriado", totalFinde);  // Usa el mismo nombre que en el segundo método        resultado.put("totalAusencias", totalAusencias);
        resultado.put("totalLlegadasTarde", totalLlegadasTarde);
        resultado.put("diasTrabajados", diasTrabajados);
        resultado.put("minutosTardeTotales", minutosTardeTotales);
        resultado.put("totalAusencias", totalAusencias);

        return resultado;
    }

    public List<ResumenAsistenciasDTO> generarResumenTotalesPorLog(Long logId, Empleado.TipoContrato tipoContrato) {
        LogCargaAsistencia log = logCargaRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log no encontrado"));

        LocalDate desde = log.getDesde();
        LocalDate hasta = log.getHasta();

        // Filtrar empleados por tipo de contrato si se especifica
        List<Empleado> empleados = tipoContrato != null
                ? empleadoRepo.findByTipoContrato(tipoContrato)
                : empleadoRepo.findAll();

        List<ResumenAsistenciasDTO> resumen = new ArrayList<>();

        for (Empleado empleado : empleados) {
            Map<String, Object> datos = generarDetalleEmpleadoView(empleado.getDni(), desde, hasta);

            // Convertir valores nulos a 0.0 para evitar NullPointerException
            double totalHoras = datos.get("totalHoras") != null ? (double) datos.get("totalHoras") : 0.0;
            double totalNormales = datos.get("totalNormales") != null ? (double) datos.get("totalNormales") : 0.0;
            double totalExtras = datos.get("totalExtras") != null ? (double) datos.get("totalExtras") : 0.0;
            double totalFindeFeriado = datos.get("totalFindeFeriado") != null ? (double) datos.get("totalFindeFeriado") : 0.0;
            long totalAusencias = datos.get("totalAusencias") != null ? (long) datos.get("totalAusencias") : 0L;
            long totalLlegadasTarde = datos.get("totalLlegadasTarde") != null ? (long) datos.get("totalLlegadasTarde") : 0L;

            ResumenAsistenciasDTO dto = new ResumenAsistenciasDTO(
                    empleado.getDni(),
                    empleado.getNombre(),
                    empleado.getApellido(),
                    totalHoras,
                    totalNormales,
                    totalExtras,
                    totalFindeFeriado,
                    totalAusencias,
                    totalLlegadasTarde,
                    empleado.getTipoContrato()

            );

            resumen.add(dto);
        }

        return resumen;
    }

    private RegistroAsistencia.TipoHora detectarTipoHora(Empleado emp, LocalDate fecha, int orden) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return RegistroAsistencia.TipoHora.FIN_SEMANA;
        }
        return orden < 2 ? RegistroAsistencia.TipoHora.NORMAL : RegistroAsistencia.TipoHora.EXTRA;

    }

    @Transactional
    public void editarHorariosDia(EdicionHorarioDTO edicionDTO) {
        // Validaciones básicas
        if (edicionDTO.getHoraEntrada1().isAfter(edicionDTO.getHoraSalida1())) {
            throw new IllegalArgumentException("La hora de entrada principal no puede ser posterior a la de salida");
        }

        if (edicionDTO.getHoraEntrada2() != null && edicionDTO.getHoraSalida2() != null &&
                edicionDTO.getHoraEntrada2().isAfter(edicionDTO.getHoraSalida2())) {
            throw new IllegalArgumentException("La hora de entrada secundaria no puede ser posterior a la de salida");
        }

        Empleado empleado = empleadoRepo.findByDni(edicionDTO.getDni())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Eliminar registros existentes para esa fecha
        List<RegistroAsistencia> registros = registroRepository
                .findByEmpleadoDniAndFecha(edicionDTO.getDni(), edicionDTO.getFecha());
        registroRepository.deleteAll(registros);

        // Crear registros para el primer horario
        RegistroAsistencia entrada1 = new RegistroAsistencia();
        entrada1.setEmpleado(empleado);
        entrada1.setFecha(edicionDTO.getFecha());
        entrada1.setHora(edicionDTO.getHoraEntrada1());
        entrada1.setOrdenDia(1);
        entrada1.setTipoHora(detectarTipoHora(empleado, edicionDTO.getFecha(), 0));
        entrada1.setMotivoCambio(edicionDTO.getMotivoCambio());

        RegistroAsistencia salida1 = new RegistroAsistencia();
        salida1.setEmpleado(empleado);
        salida1.setFecha(edicionDTO.getFecha());
        salida1.setHora(edicionDTO.getHoraSalida1());
        salida1.setOrdenDia(2);
        salida1.setTipoHora(detectarTipoHora(empleado, edicionDTO.getFecha(), 1));
        salida1.setMotivoCambio(edicionDTO.getMotivoCambio());

        registroRepository.save(entrada1);
        registroRepository.save(salida1);

        // Crear registros para el segundo horario (si existe)
        if (edicionDTO.getHoraEntrada2() != null && edicionDTO.getHoraSalida2() != null) {
            RegistroAsistencia entrada2 = new RegistroAsistencia();
            entrada2.setEmpleado(empleado);
            entrada2.setFecha(edicionDTO.getFecha());
            entrada2.setHora(edicionDTO.getHoraEntrada2());
            entrada2.setOrdenDia(3);
            entrada2.setTipoHora(detectarTipoHora(empleado, edicionDTO.getFecha(), 2));
            entrada2.setMotivoCambio(edicionDTO.getMotivoCambio());

            RegistroAsistencia salida2 = new RegistroAsistencia();
            salida2.setEmpleado(empleado);
            salida2.setFecha(edicionDTO.getFecha());
            salida2.setHora(edicionDTO.getHoraSalida2());
            salida2.setOrdenDia(4);
            salida2.setTipoHora(detectarTipoHora(empleado, edicionDTO.getFecha(), 3));
            salida2.setMotivoCambio(edicionDTO.getMotivoCambio());

            registroRepository.save(entrada2);
            registroRepository.save(salida2);
        }
    }

    public EdicionHorarioDTO prepararEdicionHorario(String dni, LocalDate fecha) {
        Empleado empleado = empleadoRepo.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        List<RegistroAsistencia> registros = registroRepository.findByEmpleadoDniAndFecha(dni, fecha)
                .stream()
                .sorted(Comparator.comparing(RegistroAsistencia::getHora))
                .toList();

        EdicionHorarioDTO dto = new EdicionHorarioDTO();
        dto.setDni(dni);
        dto.setFecha(fecha);

        // Procesar primer horario (entrada1 y salida1)
        if (registros.size() >= 2) {
            dto.setHoraEntrada1(registros.get(0).getHora());
            dto.setHoraSalida1(registros.get(1).getHora());
        } else {
            dto.setHoraEntrada1(empleado.getHoraEntrada());
            dto.setHoraSalida1(empleado.getHoraSalida());
        }

        // Procesar segundo horario (entrada2 y salida2)
        if (registros.size() >= 4) {
            dto.setHoraEntrada2(registros.get(2).getHora());
            dto.setHoraSalida2(registros.get(3).getHora());
        } else if (empleado.getHoraEntrada2() != null && empleado.getHoraSalida2() != null) {
            dto.setHoraEntrada2(empleado.getHoraEntrada2());
            dto.setHoraSalida2(empleado.getHoraSalida2());
        }

        return dto;
    }

    private String convertToBase64(byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            return Base64.getEncoder().encodeToString(imageBytes);
        }
        return null;
    }
}

