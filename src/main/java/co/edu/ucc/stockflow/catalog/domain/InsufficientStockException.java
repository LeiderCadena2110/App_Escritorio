package co.edu.ucc.stockflow.catalog.domain;

import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;

/** Trying to take out more units than there are in stock (see {@link Product#checkCanTakeOut(int)}). */
public class InsufficientStockException extends BusinessException {

    private final String sku;
    private final int available;
    private final int requested;

    public InsufficientStockException(Product product, int requested) {
        super(product.getStock() <= 0 ? ErrorCode.STOCK_SOLD_OUT : ErrorCode.STOCK_INSUFFICIENT,
                product.getSku(), product.getName(), product.getStock(), requested, product.getUnit());
        this.sku = product.getSku();
        this.available = product.getStock();
        this.requested = requested;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
