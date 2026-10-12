package co.edu.ucc.reportes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un ajuste guardado como clave y valor (carpeta de salida, tema, idioma...). */
@Entity
@Table(name = "ajuste")
public class Ajuste {

    @Id
    @Column(length = 100)
    private String clave;

    @Column(length = 2000)
    private String valor;

    protected Ajuste() {
    }

    public Ajuste(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
