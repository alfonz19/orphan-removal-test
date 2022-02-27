package alfonz19.orphanRemovalTest.jpa.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(of = "tleCode")
@NoArgsConstructor
@Entity
@Table(name = "TLE")
public class TopLevelEntity {

    @Id
    @Column(name = "tle_code", nullable = false)
    private String tleCode;

    @OneToMany(mappedBy = "tle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCode> items = new ArrayList<>();

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{\n");
        sb.append("\titemEntities=")
                .append(items.stream()
                        .map(ItemCode::toString)
                        .map(e->"\n\t\t- "+e)
                        .collect(Collectors.joining(",", "[", "\n\t]")))
                .append("\n");
        sb.append('}');
        return sb.toString();
    }
}
