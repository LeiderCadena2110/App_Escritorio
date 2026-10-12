package co.edu.ucc.reportes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Perfil: plantilla + mapeo + modo, para repetir el mismo reporte con un archivo nuevo. */
@Entity
@Table(name = "perfil_mapeo")
public class PerfilMapeo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plantilla_id")
    private Plantilla plantilla;

    /** Líneas "marcador=columna". */
    @Column(length = 8000)
    private String mapeo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoSalida modo;

    @Column(name = "columna_nombre", length = 200)
    private String columnaNombre;

    @Column(nullable = false)
    private LocalDateTime creado;

    @Column(name = "ultimo_uso")
    private LocalDateTime ultimoUso;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Plantilla getPlantilla() { return plantilla; }
    public void setPlantilla(Plantilla plantilla) { this.plantilla = plantilla; }
    public ModoSalida getModo() { return modo; }
    public void setModo(ModoSalida modo) { this.modo = modo; }
    public String getColumnaNombre() { return columnaNombre; }
    public void setColumnaNombre(String columnaNombre) { this.columnaNombre = columnaNombre; }
    public LocalDateTime getCreado() { return creado; }
    public void setCreado(LocalDateTime creado) { this.creado = creado; }
    public LocalDateTime getUltimoUso() { return ultimoUso; }
    public void setUltimoUso(LocalDateTime ultimoUso) { this.ultimoUso = ultimoUso; }

    public Map<String, String> getMapeo() {
        Map<String, String> m = new LinkedHashMap<>();
        if (mapeo == null) return m;
        for (String linea : mapeo.split("\n")) {
            int i = linea.indexOf('=');
            if (i > 0) m.put(linea.substring(0, i), linea.substring(i + 1));
        }
        return m;
    }

    public void setMapeo(Map<String, String> m) {
        StringBuilder sb = new StringBuilder();
        m.forEach((k, v) -> {
            if (v != null) sb.append(k).append('=').append(v).append('\n');
        });
        this.mapeo = sb.toString();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
