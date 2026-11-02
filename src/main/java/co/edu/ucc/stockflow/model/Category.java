package co.edu.ucc.stockflow.model;

import java.util.Objects;

/** Product group, for example "Sports shoes" or "Accessories". */
public class Category {

    private Long id;
    private String name;
    private String description;

    public Category() {
    }

    public Category(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Category c && id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** ComboBox and ListView show the name. */
    @Override
    public String toString() {
        return name;
    }
}
