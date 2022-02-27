package alfonz19.orphanRemovalTest.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@EqualsAndHashCode(of = "pk")
@NoArgsConstructor
@Entity
@Table(name = "ITEM_CODES")
public class ItemCode {

    @EmbeddedId
    private PK pk;

    @ManyToOne
    @JoinColumn(name = "tle_code")
    @MapsId("tleCode")
    private TopLevelEntity tle;

    @Override
    public String toString() {
        return "{" + "itemCode=" + pk.getItemCode() + '}';
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Embeddable
    public static class PK implements Serializable {

        @Column(name = "item_code", nullable = false)
        private String itemCode;
        @Column(name = "tle_code", nullable = false)
        private String tleCode;


    }
}
