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

/** Registro del historial: cada vez que se generan informes. */
@Entity
@Table(name = "ejecucion")
public class Ejecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "plantilla_nombre", nullable = false, length = 200)
    private String plantillaNombre;

    @Column(name = "archivo_datos", nullable = false, length = 1000)
    private String archivoDatos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoSalida modo;

    @Column(nullable = false, length = 10)
    private String formato;

    @Column(name = "carpeta_salida", nullable = false, length = 1000)
    private String carpetaSalida;

    @Column(name = "filas_procesadas", nullable = false)
    private int filasProcesadas;

    @Column(name = "informes_generados", nullable = false)
    private int informesGenerados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEjecucion estado;

    @Column(length = 8000)
    private String mensajes;

    @Column(name = "duracion_ms", nullable = false)
    private long duracionMs;

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getPlantillaNombre() { return plantillaNombre; }
    public void setPlantillaNombre(String plantillaNombre) { this.plantillaNombre = plantillaNombre; }
    public String getArchivoDatos() { return archivoDatos; }
    public void setArchivoDatos(String archivoDatos) { this.archivoDatos = archivoDatos; }
    public ModoSalida getModo() { return modo; }
    public void setModo(ModoSalida modo) { this.modo = modo; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public String getCarpetaSalida() { return carpetaSalida; }
    public void setCarpetaSalida(String carpetaSalida) { this.carpetaSalida = carpetaSalida; }
    public int getFilasProcesadas() { return filasProcesadas; }
    public void setFilasProcesadas(int filasProcesadas) { this.filasProcesadas = filasProcesadas; }
    public int getInformesGenerados() { return informesGenerados; }
    public void setInformesGenerados(int informesGenerados) { this.informesGenerados = informesGenerados; }
    public EstadoEjecucion getEstado() { return estado; }
    public void setEstado(EstadoEjecucion estado) { this.estado = estado; }
    public String getMensajes() { return mensajes; }
    public void setMensajes(String mensajes) { this.mensajes = mensajes; }
    public long getDuracionMs() { return duracionMs; }
    public void setDuracionMs(long duracionMs) { this.duracionMs = duracionMs; }
}
