package co.edu.ucc.reportes.service;

/**
 * Un problema encontrado en los datos, con la fila (como se ve en el archivo) y la columna.
 * fila o columna pueden ser null cuando el problema es de todo el archivo o de toda la columna.
 */
public record Problema(Gravedad gravedad, Integer fila, String columna, String mensaje) {

    public static Problema aviso(Integer fila, String columna, String mensaje) {
        return new Problema(Gravedad.AVISO, fila, columna, mensaje);
    }

    public static Problema error(Integer fila, String columna, String mensaje) {
        return new Problema(Gravedad.ERROR, fila, columna, mensaje);
    }
}
