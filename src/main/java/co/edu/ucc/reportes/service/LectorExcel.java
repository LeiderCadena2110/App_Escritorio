package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.i18n.Textos;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/** Lee .xlsx (y .xls): lista las hojas y convierte cada celda en el texto que irá al informe. */
public final class LectorExcel {

    private LectorExcel() {
    }

    public static List<String> hojas(Path archivo) throws IOException {
        try (Workbook libro = WorkbookFactory.create(archivo.toFile(), null, true)) {
            List<String> nombres = new ArrayList<>();
            for (int i = 0; i < libro.getNumberOfSheets(); i++) {
                if (!libro.isSheetHidden(i) && !libro.isSheetVeryHidden(i)) nombres.add(libro.getSheetName(i));
            }
            return nombres;
        }
    }

    public static TablaDatos leer(Path archivo, String hoja, FormatoValores formato) throws IOException {
        try (Workbook libro = WorkbookFactory.create(archivo.toFile(), null, true)) {
            Sheet sheet = hoja == null ? libro.getSheetAt(0) : libro.getSheet(hoja);
            if (sheet == null) throw new IOException(Textos.t("datos.hojaNoExiste", hoja));
            DataFormatter conFormato = new DataFormatter(formato.localeNumeros());
            FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();

            List<List<String>> crudas = new ArrayList<>();
            List<Integer> numeros = new ArrayList<>();
            for (Row fila : sheet) {
                List<String> valores = new ArrayList<>();
                short ultima = fila.getLastCellNum();
                for (int c = 0; c < Math.max(ultima, 0); c++) {
                    Cell celda = fila.getCell(c);
                    valores.add(celda == null ? "" : texto(celda, conFormato, evaluador, formato));
                }
                crudas.add(valores);
                numeros.add(fila.getRowNum() + 1);
            }
            String detalle = Textos.t("datos.detalleExcel", sheet.getSheetName());
            return ArmadorTabla.armar(archivo, sheet.getSheetName(), detalle, crudas, numeros);
        }
    }

    static String texto(Cell celda, DataFormatter conFormato, FormulaEvaluator evaluador, FormatoValores formato) {
        CellType tipo = celda.getCellType();
        double numero = 0;
        String texto = null;
        boolean logico = false;
        if (tipo == CellType.FORMULA) {
            // Se calcula la fórmula; si no se puede, se usa el último valor que guardó Excel
            try {
                CellValue v = evaluador.evaluate(celda);
                tipo = v.getCellType();
                numero = tipo == CellType.NUMERIC ? v.getNumberValue() : 0;
                texto = tipo == CellType.STRING ? v.getStringValue() : null;
                logico = tipo == CellType.BOOLEAN && v.getBooleanValue();
            } catch (RuntimeException e) {
                tipo = celda.getCachedFormulaResultType();
                if (tipo == CellType.NUMERIC) numero = celda.getNumericCellValue();
                if (tipo == CellType.STRING) texto = celda.getStringCellValue();
                if (tipo == CellType.BOOLEAN) logico = celda.getBooleanCellValue();
            }
        } else if (tipo == CellType.NUMERIC) {
            numero = celda.getNumericCellValue();
        } else if (tipo == CellType.STRING) {
            texto = celda.getStringCellValue();
        } else if (tipo == CellType.BOOLEAN) {
            logico = celda.getBooleanCellValue();
        }

        switch (tipo) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(celda)) {
                    LocalDateTime f = LocalDateTime.ofInstant(DateUtil.getJavaDate(numero).toInstant(), ZoneId.systemDefault());
                    return formato.fecha(f);
                }
                String patron = celda.getCellStyle() == null ? "General" : celda.getCellStyle().getDataFormatString();
                if (patron == null || patron.equalsIgnoreCase("General")) {
                    return formato.numeroGeneral(numero);
                }
                // Respeta el formato que el usuario puso en Excel (moneda, miles, %), con su configuración regional
                return conFormato.formatRawCellContents(numero, celda.getCellStyle().getDataFormat(), patron);
            case STRING:
                return texto == null ? "" : texto;
            case BOOLEAN:
                return logico ? Textos.t("valor.si") : Textos.t("valor.no");
            default:
                return "";
        }
    }
}
