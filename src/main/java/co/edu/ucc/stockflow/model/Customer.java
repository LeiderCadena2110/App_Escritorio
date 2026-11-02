package co.edu.ucc.stockflow.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Person or company that buys from the business. */
public class Customer {

    /** Document number of the generic "final consumer" customer (created on first start). */
    public static final String FINAL_CONSUMER_DOCUMENT = "222222222222";

    private Long id;
    private String documentType = "CC";
    private String documentNumber;
    private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt;

    public boolean isFinalConsumer() {
        return FINAL_CONSUMER_DOCUMENT.equals(documentNumber);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Customer c && id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return name + " (" + documentType + " " + documentNumber + ")";
    }
}
