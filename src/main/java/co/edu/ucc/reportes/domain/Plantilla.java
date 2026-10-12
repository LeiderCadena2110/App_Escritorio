package co.edu.ucc.reportes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Plantilla registrada: un .docx del usuario o una plantilla HTML de la app. */
@Entity
@Table(name = "plantilla")
public class Plantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPlantilla tipo;

    /** DOCX: ruta de la copia guardada por la app. HTML: nombre del recurso interno. */
    @Column(nullable = false, length = 1000)
    private String ruta;

    /** Marcadores {{campo}} separados por salto de línea. */
    @Column(length = 4000)
    private String marcadores;

    /** Columnas [campo] de la tabla {{filas}} (solo Word). */
    @Column(name = "columnas_tabla", length = 4000)
    private String columnasTabla;

    @Column(name = "tiene_tabla", nullable = false)
    private boolean tieneTabla;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private boolean integrada;

    @Column(nullable = false)
    private LocalDateTime creada;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoPlantilla getTipo() { return tipo; }
    public void setTipo(TipoPlantilla tipo) { this.tipo = tipo; }
    public String getRuta() { return ruta; }
    public void setRuta(String ruta) { this.ruta = ruta; }
    public boolean isTieneTabla() { return tieneTabla; }
    public void setTieneTabla(boolean tieneTabla) { this.tieneTabla = tieneTabla; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isIntegrada() { return integrada; }
    public void setIntegrada(boolean integrada) { this.integrada = integrada; }
    public LocalDateTime getCreada() { return creada; }
    public void setCreada(LocalDateTime creada) { this.creada = creada; }

    public List<String> listaMarcadores() { return dividir(marcadores); }
    public void setListaMarcadores(List<String> lista) { this.marcadores = String.join("\n", lista); }
    public List<String> listaColumnasTabla() { return dividir(columnasTabla); }
    public void setListaColumnasTabla(List<String> lista) { this.columnasTabla = String.join("\n", lista); }

    /** Todo lo que hay que relacionar con una columna: marcadores y columnas de la tabla. */
    public List<String> camposPorMapear() {
        List<String> todos = new ArrayList<>(listaMarcadores());
        for (String c : listaColumnasTabla()) {
            if (!todos.contains(c)) todos.add(c);
        }
        return todos;
    }

    private static List<String> dividir(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
