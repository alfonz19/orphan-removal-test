package alfonz19.orphanRemovalTest.jpa;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("squid:S00119")
@NoRepositoryBean
public interface ExtendedJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    @Transactional
    <S extends T> S persist(S entity);

    @Transactional
    <S extends T> S merge(S entity);

    boolean isManaged(T entity);

}
